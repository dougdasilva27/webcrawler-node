package br.com.lett.crawlernode.crawlers.corecontent.colombia;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
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

   private static final String API_TOKEN = "f0b269756fc30c5adce554a04b52ec7b";
   private static final String WEB_SAFE_TOKEN = "ahZzfnN0dW5uaW5nLWJhc2UtMTY0NDAycl0LEgRVc2VyIiQ4ZDFkMDhlMy04NDMzLTRiY2MtYjM2Mi1hNWFhNmUyNGJjZGEMCxIFVG9rZW4iJDk5Njg3ODMyLWJkN2QtNDgyOS1iZjdlLTkyN2VlYWQ3NGJlNgw";
   private static final String PRODUCTS_API_URL = "https://stunning-base-164402.uc.r.appspot.com/_ah/api/productEndpoint/getItem?idItem=";

   private static final String RATING_API_KEY = "caRsE2BkjeLAPlvqv4kY8SPNrY032XNfzo6M2cKaZgNjY";
   private static final String RATING_API_URL = "https://api.bazaarvoice.com/data/display/0.2alpha/product/summary";

   public ColombiaFarmatodoCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
   }

   public Object fetch() {
      String url = session.getOriginalURL();

      if (url.contains("producto/")) {
         String id = CommonMethods.getLast(url.split("producto/")).split("-")[0].trim();
         if (!id.isEmpty()) {
            String urlApi =
                  PRODUCTS_API_URL + id + "&idStoreGroup=26&token=" + API_TOKEN + "&tokenIdWebSafe=" + WEB_SAFE_TOKEN + "&key=AIzaSyDASDi-v-kJzulGnaRwT7sAfG44KEqaudA";

            Map<String, String> headers = new HashMap<>();
            headers.put("referer", " https://www.farmatodo.com.co/producto/" + id);

            Request request = RequestBuilder.create().setUrl(urlApi).setCookies(cookies).setHeaders(headers).build();
            String page = new FetcherDataFetcher().get(session, request).getBody();
            page = Normalizer.normalize(page, Normalizer.Form.NFD);
            return CrawlerUtils.stringToJson(page);
         }
      }

      return new JSONObject();
   }

   public List<Product> extractInformation(JSONObject json) throws Exception {
      List<Product> products = new ArrayList<>();

      System.err.println(json);
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
         String secondaryImages = scrapSecondaryImages(json);
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

      if (name != null) {
         name += " - " + JSONUtils.getStringValue(json, "description");
      }

      return name;
   }

   private String scrapSecondaryImages(JSONObject json) {
      String secondaryImages = null;

      JSONArray images = JSONUtils.getJSONArrayValue(json, "listUrlImages");
      if (images.length() > 0) {
         secondaryImages = images.toString();
      }

      return secondaryImages;
   }

   private Prices scrapPrices(JSONObject json, Float price) {
      Prices prices = new Prices();

      if (price != null) {
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

      if (!reviewJson.keySet().isEmpty()) {
         Integer totalRating = JSONUtils.getIntegerValueFromJSON(reviewJson, "numReviews", 0);
         Double avgRating = JSONUtils.getDoubleValueFromJSON(JSONUtils.getJSONValue(reviewJson, "primaryRating"), "average", true);
         AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(reviewJson);

         ratingsReviews.setTotalRating(totalRating);
         ratingsReviews.setTotalWrittenReviews(totalRating);
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
      for (Object obj : distribution) {
         if (obj instanceof JSONObject) {
            JSONObject starJson = (JSONObject) obj;

            Integer star = JSONUtils.getIntegerValueFromJSON(starJson, "key", -1);
            switch (star) {
               case 1:
                  star1 = JSONUtils.getIntegerValueFromJSON(starJson, "count", 0);
                  break;
               case 2:
                  star2 = JSONUtils.getIntegerValueFromJSON(starJson, "count", 0);
                  break;
               case 3:
                  star3 = JSONUtils.getIntegerValueFromJSON(starJson, "count", 0);
                  break;
               case 4:
                  star4 = JSONUtils.getIntegerValueFromJSON(starJson, "count", 0);
                  break;
               case 5:
                  star5 = JSONUtils.getIntegerValueFromJSON(starJson, "count", 0);
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
