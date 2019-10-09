package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.YotpoRatingReviewCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

public class BrasilMoblyCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.mobly.com.br/";
  private static final String MAIN_SELLER_NAME_LOWER = "mobly";

  public BrasilMoblyCrawler(Session session) {
    super(session);
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

      String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#configSku", "value");
      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".add-wishlistsel-product-move-to-wishlist", "data-simplesku");
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.prd-title", true);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb ul li a");
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".product-thumbs .images img", Arrays.asList("data-image-big",
          "data-image-product", "data-image-src"), "https", "staticmobly.akamaized.net");
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".product-thumbs .images img", Arrays.asList("data-image-big",
          "data-image-product", "data-image-src"), "https", "staticmobly.akamaized.net", primaryImage);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#product-attributes", ".tab-box.description-text article"));
      RatingsReviews ratingReviews = scrapRating(doc);

      JSONObject pricesJson = scrapPricesJson(doc, internalPid, internalId);

      Map<String, Prices> marketplaceMap = crawlMarketplace(doc, pricesJson);
      Prices prices = CrawlerUtils.getPrices(marketplaceMap, Arrays.asList(MAIN_SELLER_NAME_LOWER));
      boolean available = CrawlerUtils.getAvailabilityFromMarketplaceMap(marketplaceMap, Arrays.asList(MAIN_SELLER_NAME_LOWER));
      Float price = CrawlerUtils.extractPriceFromPrices(prices, Card.VISA);
      Marketplace marketplace = CrawlerUtils.assembleMarketplaceFromMap(marketplaceMap, Arrays.asList(MAIN_SELLER_NAME_LOWER), Card.VISA, session);

      // Creating the product
      Product product = ProductBuilder.create()
          .setUrl(session.getOriginalURL())
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


    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  private boolean isProductPage(Document document) {
    return !document.select("#product-info").isEmpty();
  }


  private Map<String, Prices> crawlMarketplace(Document doc, JSONObject jsonPrices) {
    Map<String, Prices> marketplaces = new HashMap<>();

    String seller = CrawlerUtils.scrapStringSimpleInfo(doc, ".prd-supplier", false);
    if (seller != null) {
      seller = seller.toLowerCase().trim();
    } else {
      seller = MAIN_SELLER_NAME_LOWER;
    }

    boolean availableToBuy = jsonPrices.has("stock_available")
        && jsonPrices.get("stock_available") instanceof Boolean
        && jsonPrices.getBoolean("stock_available");

    if (availableToBuy) {
      marketplaces.put(seller, crawlPrices(jsonPrices));
    }

    return marketplaces;
  }


  private Prices crawlPrices(JSONObject jsonPrices) {
    Prices prices = new Prices();

    Float price = JSONUtils.getFloatValueFromJSON(jsonPrices, "price", false);
    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new HashMap<>();
      installmentPriceMap.put(1, price);
      prices.setBankTicketPrice(price);

      Integer installment = JSONUtils.getIntegerValueFromJSON(jsonPrices, "installmentsCount", null);
      Float value = JSONUtils.getFloatValueFromJSON(jsonPrices, "installmentsValue", false);

      if (installment != null && value != null) {
        installmentPriceMap.put(installment, value);
      }

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
    }

    return prices;
  }

  private RatingsReviews scrapRating(Document doc) {
    RatingsReviews ratingReviews = new RatingsReviews();
    ratingReviews.setDate(session.getDate());

    String yotpoId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-reviews .yotpo", "data-product-id");

    YotpoRatingReviewCrawler yotpo = new YotpoRatingReviewCrawler(session, cookies, logger);
    Document apiDoc = yotpo.extractRatingsFromYotpo(this.session.getOriginalURL(), yotpoId, fetchAppKey(dataFetcher), dataFetcher);

    Integer totalNumOfEvaluations = CrawlerUtils.scrapIntegerFromHtml(apiDoc, "a.text-m", true, 0);
    Double avgRating = CrawlerUtils.scrapDoublePriceFromHtml(apiDoc, ".yotpo-bottomline .sr-only", null, true, '.', session);

    ratingReviews.setTotalRating(totalNumOfEvaluations);
    ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
    ratingReviews.setAverageOverallRating(avgRating);

    return ratingReviews;
  }

  /**
   * Method to fetch App Key of Yotpo API.
   * 
   * @param dataFetcher {@link DataFetcher} object to fetch page.
   * @return
   */
  private String fetchAppKey(DataFetcher dataFetcher) {

    String url = "https://www.mobly.com.br/static/jsConfiguration/?v2=1556533989";

    Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
    String response = dataFetcher.get(session, request).getBody().trim();

    return CrawlerUtils.extractSpecificStringFromScript(response, "YOTPO_URL_KEY = 'staticw2.yotpo.com/", false, "/widget.js';", false);
  }

  private JSONObject scrapPricesJson(Document doc, String internalPid, String internalId) {
    JSONObject jsonPrices = new JSONObject();

    Element scriptElement = doc.selectFirst("#lazyJavascriptInFileCode");
    if (scriptElement != null) {
      String script = scriptElement.outerHtml();
      String indexPid = "detail.priceStore[\"" + internalPid + "\"] =";

      if (script.contains(indexPid) && script.contains(";")) {
        int x = script.indexOf(indexPid) + indexPid.length();
        int y = script.indexOf(';', x);

        JSONObject jsonProduct = JSONUtils.stringToJson(script.substring(x, y).trim());

        if (jsonProduct.has("prices") && !jsonProduct.isNull("prices")) {
          JSONObject pricesJson = jsonProduct.getJSONObject("prices");
          if (pricesJson.has(internalId)) {
            jsonPrices = pricesJson.getJSONObject(internalId);
          }
        }
      }
    }

    return jsonPrices;
  }
}
