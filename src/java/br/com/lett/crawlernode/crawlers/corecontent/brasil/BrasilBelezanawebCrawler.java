package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import br.com.lett.crawlernode.util.Pair;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

/**
 * Date: 27/09/2018
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilBelezanawebCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.belezanaweb.com.br/";

  public BrasilBelezanawebCrawler(Session session) {
    super(session);
    super.config.setMustSendRatingToKinesis(true);
    super.config.setFetcher(FetchMode.FETCHER);
  }

  @Override
  public boolean shouldVisit() {
    String href = session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-sku", true);
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".nproduct-title", false);
      Float price = crawlPrice(doc);
      Prices prices = crawlPrices(price, doc);
      boolean available = !doc.select(".product-buy > a").isEmpty();
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb .breadcrumb-item:not(:first-child) > a", false);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".product-image-wrapper > img", Arrays.asList("data-zoom-image", "src"),
          "https:", "res.cloudinary.com");
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".gallery-lightbox .product-image-wrapper > img",
          Arrays.asList("data-zoom-image", "src"), "https:", "res.cloudinary.com", primaryImage);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".product-description", ".product-characteristics"));
      RatingsReviews rating = scrapRating(doc);

      // Creating the product
      Product product = ProductBuilder.create()
          .setUrl(session.getOriginalURL())
          .setInternalId(internalId)
          .setName(name)
          .setPrice(price)
          .setPrices(prices)
          .setAvailable(available)
          .setCategory1(categories.getCategory(0))
          .setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2))
          .setPrimaryImage(primaryImage)
          .setSecondaryImages(secondaryImages)
          .setDescription(description)
          .setMarketplace(new Marketplace())
          .setRatingReviews(rating)
          .build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }

  private boolean isProductPage(Document doc) {
    return !doc.select(".product-sku").isEmpty();
  }

  private RatingsReviews scrapRating(Document doc) {
    RatingsReviews ratingReviews = new RatingsReviews();
    ratingReviews.setDate(session.getDate());

    Integer totalNumOfEvaluations = CrawlerUtils.scrapIntegerFromHtml(doc, ".rating-container .rating-count", false, 0);
    Double avgRating = getTotalAvgRating(doc);

    ratingReviews.setTotalRating(totalNumOfEvaluations);
    ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
    ratingReviews.setAverageOverallRating(avgRating);

    return ratingReviews;
  }

  private Double getTotalAvgRating(Document doc) {
    Double avgRating = 0d;

    Element avg = doc.selectFirst(".rating-value-container");

    if (avg != null) {
      String text = avg.ownText().replaceAll("[^0-9.]", "").trim();

      if (!text.isEmpty()) {
        avgRating = Double.parseDouble(text);
      }
    }

    return avgRating;
  }

  private Float crawlPrice(Document document) {
    Float price = null;

    Element salePriceElement = document.selectFirst(".nproduct-price-value[content]");
    if (salePriceElement != null) {
      price = Float.parseFloat(salePriceElement.attr("content"));
    }

    return price;
  }

  /**
   * In the time when this crawler was made, this market hasn't installments informations
   * 
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price, Document doc) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);

      Element priceFrom = doc.selectFirst(".nproduct-price-max");
      if (priceFrom != null) {
        prices.setPriceFrom(MathUtils.parseDoubleWithComma(priceFrom.text()));
      }

      Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(".nproduct-price-installments", doc, false);
      if (!pair.isAnyValueNull()) {
        installmentPriceMap.put(pair.getFirst(), pair.getSecond());
      }

      prices.setBankTicketPrice(price);

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
    }

    return prices;
  }

}
