package br.com.lett.crawlernode.crawlers.corecontent.chile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

/**
 * Date: 20/12/2018
 * 
 * @author Gabriel Dornelas
 *
 */
public class ChileParisCrawler extends Crawler {

  public static final String HOST = "www2.paris.cl";
  private static final String HOME_PAGE = "https://" + HOST + "/";

  public ChileParisCrawler(Session session) {
    super(session);
    super.config.setMustSendRatingToKinesis(true);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = crawlInternalId(doc);
      String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-sku", true);
      String name = crawlName(doc);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "a.breadcrumb-element");
      Float price = crawlPrice(doc);
      Prices prices = crawlPrices(price, doc);
      boolean available = crawlAvailability(doc);
      String primaryImage = crawlPrimaryImage(doc);
      String secondaryImages = crawlSecondaryImages(doc, primaryImage);
      String description = crawlDescription(doc);
      String url = CrawlerUtils.getRedirectedUrl(session.getOriginalURL(), session);
      RatingsReviews ratingReviews = null;

      Product product = ProductBuilder.create().setUrl(url).setInternalId(internalId).setInternalPid(internalPid).setName(name).setPrice(price)
          .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setMarketplace(new Marketplace()).setRatingReviews(ratingReviews).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }

  private boolean isProductPage(Document doc) {
    return doc.select("#pdpMain").first() != null;
  }

  private String crawlInternalId(Document doc) {
    String internalId = null;

    JSONObject product = CrawlerUtils.selectJsonFromHtml(doc, "script", "varproduct=", ";", true, false);
    if (product.has("sku")) {
      String sku = product.get("sku").toString().trim();

      if (!sku.isEmpty()) {
        internalId = sku;
      }
    }

    if (internalId == null && product.has("id")) {
      String id = product.get("id").toString().trim();

      if (!id.isEmpty()) {
        internalId = id;
      }
    }

    return internalId;
  }

  private String crawlName(Document doc) {
    String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".js-product-name", true);

    Element colorV = doc.selectFirst(".color .selected a[title]");
    if (colorV != null) {
      name += " " + CommonMethods.getLast(colorV.attr("title").split(":")).trim();
    }

    return name;
  }

  private String crawlPrimaryImage(Document doc) {
    return CrawlerUtils.scrapSimplePrimaryImage(doc, "#thumbnails li > a", Arrays.asList("href"), "https:", HOST);
  }

  private String crawlSecondaryImages(Document doc, String primaryImage) {
    return CrawlerUtils.scrapSimpleSecondaryImages(doc, "#thumbnails li > a", Arrays.asList("href"), "https:", HOST, primaryImage);
  }

  private Float crawlPrice(Document doc) {
    return CrawlerUtils.scrapSimplePriceFloat(doc, ".row-price .price-internet, .price .default-price", true);
  }

  private Prices crawlPrices(Float price, Document doc) {
    Prices prices = new Prices();

    if (price != null) {
      prices.setPriceFrom(CrawlerUtils.scrapSimplePriceDouble(doc, ".row-price .price-normal s", true));

      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);

      Map<Integer, Float> installmentPriceShopMap = new TreeMap<>();

      Float priceCencosud = CrawlerUtils.scrapSimplePriceFloat(doc, ".price .cencosud-price", true);
      installmentPriceShopMap.put(1, priceCencosud != null ? priceCencosud : price);

      Elements installments = doc.select(".table-fee tbody tr");
      for (Element e : installments) {
        Elements tds = e.select("td");
        if (tds.size() > 2) {
          String installmentNumber = tds.get(0).ownText().replaceAll("[^0-9]", "").trim();
          Float value = MathUtils.parseFloatWithComma(tds.get(1).ownText());

          if (!installmentNumber.isEmpty() && value != null) {
            installmentPriceShopMap.put(Integer.parseInt(installmentNumber), value);
          }
        }
      }

      prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceShopMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
    }

    return prices;
  }

  private boolean crawlAvailability(Document doc) {
    return !doc.select("#add-to-cart").isEmpty();
  }

  private String crawlDescription(Document doc) {
    return CrawlerUtils.scrapElementsDescription(doc, Arrays.asList("#accordion >:not(.reviews):not(.questions):not(.js-read-review-section)"));
  }
}
