package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.http.HttpHeaders;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.FetchUtilities;
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
import br.com.lett.crawlernode.util.Pair;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

/**
 * Date: 08/10/2018
 * 
 * @author Gabriel Dornelas
 *
 */
public class MercadolivreCrawler extends Crawler {

  private String homePage;
  private String mainSellerNameLower;
  private char separator;

  protected MercadolivreCrawler(Session session) {
    super(session);
    super.config.setMustSendRatingToKinesis(true);
  }

  public void setSeparator(char separator) {
    this.separator = separator;
  }

  public void setHomePage(String homePage) {
    this.homePage = homePage;
  }

  public void setMainSellerNameLower(String mainSellerNameLower) {
    this.mainSellerNameLower = mainSellerNameLower;
  }

  @Override
  public boolean shouldVisit() {
    String href = session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(homePage));
  }

  @Override
  protected Object fetch() {
    Map<String, String> headers = new HashMap<>();
    headers.put(HttpHeaders.USER_AGENT, FetchUtilities.randUserAgentWithoutChrome());

    Request request = RequestBuilder.create()
        .setUrl(session.getOriginalURL())
        .setCookies(cookies)
        .setHeaders(headers)
        .build();

    return Jsoup.parse(this.dataFetcher.get(session, request).getBody());
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input[name=itemId], #productInfo input[name=\"item_id\"]", "value");

      Map<String, Document> variations = getVariationsHtmls(doc);
      for (Entry<String, Document> entry : variations.entrySet()) {
        Document docVariation = entry.getValue();

        String variationId = CrawlerUtils.scrapStringSimpleInfoByAttribute(docVariation, "input[name=variation]", "value");

        if (variations.size() > 1 && (variationId == null || variationId.trim().isEmpty())) {
          continue;
        }

        String internalId = variationId == null || variations.size() < 2 ? internalPid : internalPid + "-" + variationId;

        String name = crawlName(docVariation);
        CategoryCollection categories = CrawlerUtils.crawlCategories(docVariation, "a.breadcrumb:not(.shortened)");
        String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(docVariation, "figure.gallery-image-container a", Arrays.asList("href"), "https:",
            "http2.mlstatic.com");
        String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(docVariation, "figure.gallery-image-container a", Arrays.asList("href"),
            "https:", "http2.mlstatic.com", primaryImage);
        String description =
            CrawlerUtils.scrapSimpleDescription(docVariation, Arrays.asList(".vip-section-specs", ".section-specs", ".item-description"));

        boolean availableToBuy = !docVariation.select(".item-actions [value=\"Comprar agora\"]").isEmpty()
            || !docVariation.select(".item-actions [value=\"Comprar ahora\"]").isEmpty();
        Map<String, Prices> marketplaceMap = availableToBuy ? crawlMarketplace(doc) : new HashMap<>();
        boolean available = availableToBuy && marketplaceMap.containsKey(mainSellerNameLower);

        Marketplace marketplace = CrawlerUtils.assembleMarketplaceFromMap(marketplaceMap, Arrays.asList(mainSellerNameLower), Card.VISA, session);
        Prices prices = available ? marketplaceMap.get(mainSellerNameLower) : new Prices();
        Float price = CrawlerUtils.extractPriceFromPrices(prices, Card.VISA);

        RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();
        ratingReviewsCollection.addRatingReviews(crawlRating(doc, internalId));
        RatingsReviews ratingReviews = ratingReviewsCollection.getRatingReviews(internalId);

        // Creating the product
        Product product = ProductBuilder.create()
            .setUrl(entry.getKey())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
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
            .setMarketplace(marketplace)
            .setRatingReviews(ratingReviews)
            .build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }

  private RatingsReviews crawlRating(Document doc, String internalId) {
    RatingsReviews ratingReviews = new RatingsReviews();
    ratingReviews.setDate(session.getDate());

    Integer totalNumOfEvaluations = getTotalNumOfRatings(doc);
    Double avgRating = getTotalAvgRating(doc);

    ratingReviews.setInternalId(internalId);
    ratingReviews.setTotalRating(totalNumOfEvaluations);
    ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
    ratingReviews.setAverageOverallRating(avgRating);

    return ratingReviews;
  }

  /**
   * 
   * @param document
   * @return
   */
  private Double getTotalAvgRating(Document doc) {
    Double avgRating = 0d;

    Element avg = doc.selectFirst(".review-summary-average");
    if (avg != null) {
      String text = avg.ownText().replaceAll("[^0-9.]", "");

      if (!text.isEmpty()) {
        avgRating = Double.parseDouble(text);
      }
    }

    return avgRating;
  }

  /**
   * Number of ratings appear in html
   * 
   * @param docRating
   * @return
   */
  private Integer getTotalNumOfRatings(Document docRating) {
    Integer totalRating = 0;
    Element totalRatingElement = docRating.selectFirst(".core-review .average-legend");

    if (totalRatingElement != null) {
      String text = totalRatingElement.text().replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        totalRating = Integer.parseInt(text);
      }
    }

    return totalRating;
  }

  private boolean isProductPage(Document doc) {
    return !doc.select(".vip-nav-bounds .layout-main").isEmpty();
  }

  private Map<String, Document> getVariationsHtmls(Document doc) {
    Map<String, Document> variations = new HashMap<>();

    String originalUrl = session.getOriginalURL();
    variations.putAll(getSizeVariationsHmtls(doc, originalUrl));

    Elements colors = doc.select(".variation-list--full li:not(.variations-selected)");
    for (Element e : colors) {
      String dataValue = e.attr("data-value");
      String url =
          originalUrl + (originalUrl.contains("?") ? "&" : "?") + "attribute=COLOR_SECONDARY_COLOR%7C" + dataValue + "&quantity=1&noIndex=true";
      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
      Document docColor = Jsoup.parse(this.dataFetcher.get(session, request).getBody());
      variations.putAll(getSizeVariationsHmtls(docColor, url));
    }

    return variations;
  }

  private Map<String, Document> getSizeVariationsHmtls(Document doc, String urlColor) {
    Map<String, Document> variations = new HashMap<>();
    variations.put(urlColor, doc);

    Elements sizes = doc.select(".variation-list li:not(.variations-selected) a.ui-list__item-option");
    for (Element e : sizes) {
      String attribute = null;
      String[] parameters = e.attr("href").split("&");
      for (String p : parameters) {
        if (p.startsWith("attribute=")) {
          attribute = p;
          break;
        }
      }

      String url = urlColor + (urlColor.contains("?") ? "&" : "?") + attribute;
      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
      Document docSize = Jsoup.parse(this.dataFetcher.get(session, request).getBody());

      variations.put(url, docSize);
    }

    return variations;
  }

  private static String crawlName(Document doc) {
    StringBuilder name = new StringBuilder();
    name.append(CrawlerUtils.scrapStringSimpleInfo(doc, "h1.item-title__primary", true));

    Element sizeElement = doc.selectFirst(".variation-list li.variations-selected");
    if (sizeElement != null) {
      name.append(" ").append(sizeElement.attr("data-title"));
    }

    Element colorElement = doc.selectFirst(".variation-list--full li.variations-selected");
    if (colorElement != null) {
      name.append(" ").append(colorElement.attr("data-title"));
    }

    return name.toString();
  }

  private Map<String, Prices> crawlMarketplace(Document doc) {
    Map<String, Prices> marketplace = new HashMap<>();

    String sellerName = mainSellerNameLower;
    Element sellerNameElement = doc.selectFirst(".official-store-info .title");

    if (sellerNameElement != null) {
      sellerName = sellerNameElement.ownText().toLowerCase().trim();
    } else {
      sellerNameElement = doc.selectFirst(".new-reputation > a");

      if (sellerNameElement != null) {
        try {
          sellerName = URLDecoder.decode(CommonMethods.getLast(sellerNameElement.attr("href").split("/")), "UTF-8").toLowerCase();
        } catch (UnsupportedEncodingException e) {
          Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
        }
      }
    }

    marketplace.put(sellerName, crawlPrices(doc));

    return marketplace;

  }

  /**
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Document doc) {
    Prices prices = new Prices();

    Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".item-price span.price-tag:not(.price-tag__del)", null, false, separator, session);
    if (price != null) {
      Map<Integer, Float> mapInstallments = new HashMap<>();
      mapInstallments.put(1, price);
      prices.setBankTicketPrice(price);

      prices.setPriceFrom(CrawlerUtils.scrapDoublePriceFromHtml(doc, "del .price-tag-fraction", null, true, ',', session));

      Elements installments = doc.select(".payment-installments");
      for (Element e : installments) {
        Element eParsed = Jsoup.parse(e.toString().replace("<sup>", ",").replace("</sup>", ""));
        Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(null, eParsed, false, "x");
        if (!pair.isAnyValueNull()) {
          mapInstallments.put(pair.getFirst(), pair.getSecond());
        }
      }

      prices.insertCardInstallment(Card.VISA.toString(), mapInstallments);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), mapInstallments);
    }

    // }

    return prices;
  }

}
