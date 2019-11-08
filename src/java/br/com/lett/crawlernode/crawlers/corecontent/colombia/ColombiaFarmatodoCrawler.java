package br.com.lett.crawlernode.crawlers.corecontent.colombia;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import models.AdvancedRatingReview;
import models.RatingsReviews;
import models.prices.Prices;

public class ColombiaFarmatodoCrawler extends Crawler {
  
  private static final String API_TOKEN = "d406fb453dc09e35fc388ae47a58447e";
  private static final String WEB_SAFE_TOKEN = "ahZzfnN0dW5uaW5nLWJhc2UtMTY0NDAycl0LEgRVc2VyIiQyZDk5MjdiYS1hYzUxLTQ2N2QtOGFlNS00MmYyNTY0MjExN2EMCxIFVG9rZW4iJDA5NTk3YWNkLTJkYjItNDYwNS1hYzBmLTcwZjlmYmQ4YjZjMgw";
  private static final String PRODUCTS_API_URL = "https://stunning-base-164402.appspot.com/_ah/api/productEndpoint/getItem";
  
  private static final String RATING_API_KEY = "caRsE2BkjeLAPlvqv4kY8SPNrY032XNfzo6M2cKaZgNjY";
  private static final String RATING_API_URL = "https://api.bazaarvoice.com/data/display/0.2alpha/product/summary";
  
  public ColombiaFarmatodoCrawler(Session session) {
    super(session);
    super.config.setMustSendRatingToKinesis(true);
  }
  
  public Object fetch() {
    String url = session.getOriginalURL();

    if (url.contains("product/")) {
      String id = CommonMethods.getLast(url.split("product/")).split("\\/")[0].trim();

      if (!id.isEmpty()) {
        String urlApi = PRODUCTS_API_URL + "?idItem=" + id + "&idStoreGroup=26&token=" + API_TOKEN + "&tokenIdWebSafe=" + WEB_SAFE_TOKEN;

        Request request = RequestBuilder.create().setUrl(urlApi).setCookies(cookies).build();
        return CrawlerUtils.stringToJson(new FetcherDataFetcher().get(session, request).getBody());
      }
    }

    return new JSONObject();
  }

  public List<Product> extractInformation(JSONObject json) throws Exception {
    List<Product> products = new ArrayList<>();
    
    if (isProductPage(json)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
      
      JSONObject seo = JSONUtils.getJSONValue(json, "seo");
      JSONObject offer = JSONUtils.getJSONValue(seo, "offers");
      
      String internalId = JSONUtils.getStringValue(seo, "sku");
      String internalPid = JSONUtils.getStringValue(json, "id");
      String name = scrapName(seo);
      Float price = JSONUtils.getFloatValueFromJSON(offer, "price", true);
      Prices prices = scrapPrices(offer, price);
      String primaryImage = JSONUtils.getStringValue(seo, "image");
      String secondaryImages = null;
      String description = JSONUtils.getStringValue(json, "largeDescription");
      Integer stock = JSONUtils.getIntegerValueFromJSON(json, "totalStock", 0);
      boolean available = stock > 0;
      RatingsReviews ratingsReviews = scrapRatingsReviews(internalPid);
          
      // Creating the product
      Product product = ProductBuilder.create()
          .setUrl(session.getOriginalURL())
          .setInternalId(internalId)
          .setInternalPid(internalPid)
          .setName(name)
          .setPrice(price)
          .setPrices(prices)
          .setAvailable(available)
          .setPrimaryImage(primaryImage)
          .setSecondaryImages(secondaryImages)
          .setDescription(description)
          .setStock(stock)
          .setRatingReviews(ratingsReviews)
          .build();
      
      products.add(product);
      
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }
    
    return products;
  }
  
  private boolean isProductPage(JSONObject json) {
    return !json.keySet().isEmpty();
  }
  
  private String scrapName(JSONObject json) {
    String name = JSONUtils.getStringValue(json, "name");
    
    if(name != null) {
      name += " - " + JSONUtils.getStringValue(json, "description");
    }
    
    return name;
  }
  
  private Prices scrapPrices(JSONObject json, Float price) {
    Prices prices = new Prices();
    
    if(price != null) {
      prices.setPriceFrom(JSONUtils.getDoubleValueFromJSON(json, "highPrice", true));
      
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);
      
      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
    }
    
    return prices;
  }
  
  private RatingsReviews scrapRatingsReviews(String productId) {
    RatingsReviews ratingsReviews = new RatingsReviews();
    ratingsReviews.setDate(session.getDate());
    
    String urlApi = RATING_API_URL + "?PassKey=" + RATING_API_KEY + "&productid=" + productId + 
        "&contentType=reviews,questions&reviewDistribution=primaryRating,recommended&rev=0&contentlocale=es*,es_CO";

    Request request = RequestBuilder.create().setUrl(urlApi).setCookies(cookies).build();
    JSONObject reviewJson = CrawlerUtils.stringToJson(new FetcherDataFetcher().get(session, request).getBody());
    reviewJson = JSONUtils.getJSONValue(reviewJson, "reviewSummary");
    
    if(!reviewJson.keySet().isEmpty()) {
      Integer totalRating = JSONUtils.getIntegerValueFromJSON(reviewJson, "numReviews", 0);
      Double avgRating = JSONUtils.getDoubleValueFromJSON(JSONUtils.getJSONValue(reviewJson, "primaryRating"), "average", true);
      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(reviewJson);
      
      ratingsReviews.setTotalRating(totalRating);
      ratingsReviews.setAverageOverallRating(avgRating == null ? 0.0f : avgRating);      
      ratingsReviews.setAdvancedRatingReview(advancedRatingReview);
    }
    
    return ratingsReviews;
  }
  
  private AdvancedRatingReview scrapAdvancedRatingReview(JSONObject json) {
    Integer star1 = 0;
    Integer star2 = 0;
    Integer star3 = 0;
    Integer star4 = 0;
    Integer star5 = 0;
    
    json = JSONUtils.getJSONValue(json, "primaryRating");
    
    JSONArray distribution = JSONUtils.getJSONArrayValue(json, "distribution");    
    for(Object obj : distribution) {
      if(obj instanceof JSONObject) {
        JSONObject starJson = (JSONObject) obj;
        
        Integer star = JSONUtils.getIntegerValueFromJSON(starJson, "key", -1);
        switch(star) {
          case 1: star1 = JSONUtils.getIntegerValueFromJSON(starJson, "count", 0); 
          break;
          case 2: star2 = JSONUtils.getIntegerValueFromJSON(starJson, "count", 0); 
          break;
          case 3: star3 = JSONUtils.getIntegerValueFromJSON(starJson, "count", 0);  
          break;
          case 4: star4 = JSONUtils.getIntegerValueFromJSON(starJson, "count", 0); 
          break;
          case 5: star5 = JSONUtils.getIntegerValueFromJSON(starJson, "count", 0); 
          break;
        }
      }
    }
    
    return new AdvancedRatingReview.Builder()
        .totalStar1(star1)
        .totalStar2(star2)
        .totalStar3(star3)
        .totalStar4(star4)
        .totalStar5(star5)
        .build();
  }
}
