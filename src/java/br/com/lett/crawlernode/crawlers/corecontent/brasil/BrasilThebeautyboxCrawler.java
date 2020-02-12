package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.YourreviewsRatingCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.AdvancedRatingReview;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.*;

public class BrasilThebeautyboxCrawler extends Crawler {

  public BrasilThebeautyboxCrawler(Session session) {
    super(session);
    this.config.setMustSendRatingToKinesis(true);
  }

  private static final String HOME_PAGE = "https://www.beautybox.com.br/";
  private static final String MAIN_SELLER_NAME_LOWER = "the beauty box";

  @Override
  public boolean shouldVisit() {
    String href = session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }


  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    String redirectUrl = CrawlerUtils.getRedirectedUrl(session.getOriginalURL(), session);
    JSONObject stateJson = CrawlerUtils.selectJsonFromHtml(doc, "script", "__STATE__ =", null, false, true);
    JSONObject productJson = scrapProductJson(stateJson, redirectUrl);

    if (productJson.has("productId")) {
      String internalPid = productJson.has("productId") && !productJson.isNull("productId") ? productJson.get("productId").toString() : null;
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "a[class^=vtex-breadcrumb]:not([href=\"/\"])");
      String description = scrapDescription(productJson);
      RatingsReviews ratingsReviews = scrapRating(internalPid);

      JSONArray items = productJson.has("items") && !productJson.isNull("items") ? productJson.getJSONArray("items") : new JSONArray();

      for (int i = 0; i < items.length(); i++) {
        JSONObject jsonSku = getInformationJson(items.getJSONObject(i), stateJson);

        String internalId = jsonSku.has("itemId") ? jsonSku.get("itemId").toString() : null;
        String name = jsonSku.has("nameComplete") ? jsonSku.get("nameComplete").toString() : null;
        Map<String, Prices> marketplaceMap = scrapMarketplace(jsonSku, stateJson);
        List<String> mainSellers = CrawlerUtils.getMainSellers(marketplaceMap, Arrays.asList(MAIN_SELLER_NAME_LOWER));
        Marketplace marketplace = CrawlerUtils.assembleMarketplaceFromMap(marketplaceMap, mainSellers, Card.VISA, session);
        boolean available = CrawlerUtils.getAvailabilityFromMarketplaceMap(marketplaceMap, mainSellers);

        List<String> images = scrapImages(jsonSku, stateJson);
        String primaryImage = !images.isEmpty() ? images.get(0) : null;
        String secondaryImages = scrapSecondaryImages(images);

        Prices prices = CrawlerUtils.getPrices(marketplaceMap, mainSellers);
        Float price = CrawlerUtils.extractPriceFromPrices(prices, Card.VISA);
        ratingsReviews.setInternalId(internalId);
        ratingsReviews.setDate(session.getDate());
        ratingsReviews.setUrl(session.getOriginalURL());
        List<String> eans = jsonSku.has("ean") ? Arrays.asList(jsonSku.get("ean").toString()) : null;

        // Creating the product
        Product product = ProductBuilder.create()
                .setUrl(session.getOriginalURL())
                .setInternalId(internalId)
                .setInternalPid(internalPid)
                .setName(name)
                .setRatingReviews(ratingsReviews.clone())
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
                .setEans(eans)
                .build();

        products.add(product);
      }
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + session.getOriginalURL());
    }

    return products;
  }

  /**
   * stateJson is like this:
   * 
   * "Product.undefined@undefined.x::celular-redmi-note-7-dual-camara-48mp5mp-128gb-4gb-negro-100163854-mp":{
   * "cacheId":"celular-redmi-note-7-dual-camara-48mp5mp-128gb-4gb-negro-100163854-mp",
   * "productName":"Celular Redmi Note 7 Dual Camara 48MP+5MP 128GB 4GB Negro",
   * "productId":"100163854", ...
   * 
   * Url:
   * https://www.exito.com/celular-redmi-note-7-dual-camara-48mp5mp-128gb-4gb-negro-100163854-mp/p
   * FirstKey:
   * 
   * Can be Product:celular-redmi-note-7-dual-camara-48mp5mp-128gb-4gb-negro-100163854-mp
   * 
   * Or
   * Product.undefined@undefined.x::celular-redmi-note-7-dual-camara-48mp5mp-128gb-4gb-negro-100163854-mp
   * 
   * We need to build this key with url path
   * 
   * @param url
   * @return
   */
  private JSONObject scrapProductJson(JSONObject stateJson, String url) {
    JSONObject jsonSku = new JSONObject();


    if (url.contains("/p")) {
      String urlPath = (url.replace(HOME_PAGE, "")).split("/p")[0];

      String key = "Product:" + urlPath;
      String specialKey = "Product.undefined@undefined.x::" + urlPath;

      if (stateJson.has(key) && !stateJson.isNull(key)) {
        jsonSku = stateJson.getJSONObject(key);
      } else if (stateJson.has(specialKey) && !stateJson.isNull(specialKey)) {
        jsonSku = stateJson.getJSONObject(specialKey);
      }
    }

    return jsonSku;
  }

  /**
   * In this market we have this case:
   * 
   * {
   * 
   * ---"Product.undefined@undefined.x::celular": {
   * 
   * ------"name": "blabla",
   * 
   * ------"description": "nada",
   * 
   * ------"items": {
   * 
   * --------"id":"Product.undefined@undefined.x::celular.items.0"
   * 
   * ------}
   * 
   * ---},
   * 
   * ---"Product.undefined@undefined.x::celular.items.0":{
   * 
   * ------"itemId": 25,
   * 
   * ------"avaiable": true
   * 
   * ---}
   * 
   * }
   * 
   * For this, we need that function for scrap items for example, but are more cases like this
   * 
   * @param json
   * @param stateJson
   * @return
   */
  private JSONObject getInformationJson(JSONObject json, JSONObject stateJson) {
    JSONObject specificJson = new JSONObject();

    if (json.has("id") && !json.isNull("id")) {
      String key = json.get("id").toString();

      if (stateJson.has(key) && !stateJson.isNull(key)) {
        specificJson = stateJson.getJSONObject(key);
      }
    }

    return specificJson;
  }

  private String scrapDescription(JSONObject json) {
    StringBuilder description = new StringBuilder();

    if (json.has("description") && !json.isNull("description")) {
      description.append(Jsoup.parse(json.get("description").toString()));
    }

    return description.toString();
  }

  private List<String> scrapImages(JSONObject skuJson, JSONObject stateJson) {
    List<String> images = new ArrayList<>();

    for (String key : skuJson.keySet()) {
      if (key.startsWith("images")) {
        JSONArray imagesArray = skuJson.getJSONArray(key);

        for (Object o : imagesArray) {
          JSONObject image = getInformationJson((JSONObject) o, stateJson);

          if (image.has("imageUrl") && !image.isNull("imageUrl")) {
            images.add(CrawlerUtils.completeUrl(image.get("imageUrl").toString(), "https", "jumbo.vteximg.com.br"));
          }
        }

        break;
      }
    }

    return images;
  }

  private String scrapSecondaryImages(List<String> images) {
    String secondaryImages = null;
    JSONArray imagesArray = new JSONArray();

    if (!images.isEmpty()) {
      images.remove(0);

      for (String image : images) {
        imagesArray.put(image);
      }
    }

    if (imagesArray.length() > 0) {
      secondaryImages = imagesArray.toString();
    }

    return secondaryImages;
  }

  private Map<String, Prices> scrapMarketplace(JSONObject jsonSku, JSONObject stateJson) {
    Map<String, Prices> map = new HashMap<>();

    if (jsonSku.has("sellers") && !jsonSku.isNull("sellers")) {
      JSONArray sellers = jsonSku.getJSONArray("sellers");

      for (Object o : sellers) {
        JSONObject seller = getInformationJson((JSONObject) o, stateJson);

        if (seller.has("sellerName") && !seller.isNull("sellerName") && seller.has("commertialOffer") && !seller.isNull("commertialOffer")) {
          Prices prices = scrapPrices(getInformationJson(seller.getJSONObject("commertialOffer"), stateJson), stateJson);

          if (!prices.isEmpty()) {
            map.put(seller.get("sellerName").toString(), prices);
          }
        }
      }
    }

    return map;
  }

  private Prices scrapPrices(JSONObject comertial, JSONObject stateJson) {
    Prices prices = new Prices();

    if (comertial.has("Price") && !comertial.isNull("Price")) {
      Float price = CrawlerUtils.getFloatValueFromJSON(comertial, "Price", true, false);

      if (price > 0) {
        Map<Integer, Float> installments = new HashMap<>();
        installments.put(1, price);
        prices.setPriceFrom(CrawlerUtils.getDoubleValueFromJSON(comertial, "ListPrice", true, false));

        if (comertial.has("Installments") && !comertial.isNull("Installments")) {
          JSONArray installmentsArray = comertial.getJSONArray("Installments");

          for (Object o : installmentsArray) {
            JSONObject installmentJson = getInformationJson((JSONObject) o, stateJson);

            Integer installmentNumber = CrawlerUtils.getIntegerValueFromJSON(installmentJson, "NumberOfInstallments", null);
            Float installmentValue = CrawlerUtils.getFloatValueFromJSON(installmentJson, "Value");

            if (installmentNumber != null && installmentValue != null) {
              installments.put(installmentNumber, installmentValue);
            }
          }
        }

        prices.insertCardInstallment(Card.VISA.toString(), installments);
        prices.insertCardInstallment(Card.SHOP_CARD.toString(), installments);
      }
    }

    return prices;
  }

  private RatingsReviews scrapRating(String internalPid) {
    RatingsReviews ratingReviews = new RatingsReviews();
    YourreviewsRatingCrawler yourReviews = new YourreviewsRatingCrawler(session, cookies, logger, "31087896-d490-4f03-b64b-5948e3fb52e5", dataFetcher);

    Document docRating = yourReviews.crawlPageRatingsFromYourViews(internalPid, "31087896-d490-4f03-b64b-5948e3fb52e5", this.dataFetcher);
    Integer totalNumOfEvaluations = CrawlerUtils.scrapIntegerFromHtml(docRating, "strong[itemprop=ratingCount]", false, 0);
    Double avgRating = getTotalAvgRatingFromYourViews(docRating);
    AdvancedRatingReview advancedRatingReview = yourReviews.getTotalStarsFromEachValue(internalPid);

    ratingReviews.setAdvancedRatingReview(advancedRatingReview);
    ratingReviews.setTotalRating(totalNumOfEvaluations);
    ratingReviews.setAverageOverallRating(avgRating);
    ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);

    return ratingReviews;
  }

  private Double getTotalAvgRatingFromYourViews(Document docRating) {
    Double avgRating = 0D;
    Double ratingOnHtml = CrawlerUtils.scrapDoublePriceFromHtml(docRating, "meta[itemprop=ratingValue]", "content", false, '.', session);

    if (ratingOnHtml != null) {
      avgRating = ratingOnHtml;
    }

    return avgRating;
  }
}
