package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpHeaders;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXCrawlersUtils;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;


public class BrasilMegamamuteCrawler extends Crawler {

   private static final String HOME_PAGE = "http://www.megamamute.com.br/";
   private static final String MAIN_SELLER_NAME_LOWER = "megamamute";

   public BrasilMegamamuteCrawler(Session session) {
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
         VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, MAIN_SELLER_NAME_LOWER, HOME_PAGE, cookies, dataFetcher);
         vtexUtil.setDiscountWithDocument(doc, ".flagPromos-v1 p[class^=flag desconto-]", false, true);

         JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);
         System.err.println(skuJson);

         String internalPid = vtexUtil.crawlInternalPid(skuJson);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".bread-crumb > ul li:not(:first-child) a");
         String description = CrawlerUtils.scrapSimpleDescription(doc,
               Arrays.asList("#product-qd-v1-description", ".product-qd-v1-description", ".productDescription", "#caracteristicas"));


         // sku data in json
         JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

         for (int i = 0; i < arraySkus.length(); i++) {
            JSONObject jsonSku = arraySkus.getJSONObject(i);

            String internalId = vtexUtil.crawlInternalId(jsonSku);
            JSONObject apiJSON = vtexUtil.crawlApi(internalId);
            String name = vtexUtil.crawlName(jsonSku, skuJson, " ");
            // String name = crawlName(jsonSku, skuJson);
            Map<String, Prices> marketplaceMap = vtexUtil.crawlMarketplace(apiJSON, internalId, true);
            Marketplace marketplace = vtexUtil.assembleMarketplaceFromMap(marketplaceMap);
            boolean available = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER);
            String primaryImage = vtexUtil.crawlPrimaryImage(apiJSON);
            String secondaryImages = vtexUtil.crawlSecondaryImages(apiJSON);
            Prices prices = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER) ? marketplaceMap.get(MAIN_SELLER_NAME_LOWER) : new Prices();
            Float price = vtexUtil.crawlMainPagePrice(prices);
            Integer stock = vtexUtil.crawlStock(apiJSON);
            RatingsReviews ratingReviews = crawlRating(doc, internalPid);

            // Creating the product
            Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
                  .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
                  .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
                  .setStock(stock).setRatingReviews(ratingReviews).setMarketplace(marketplace).build();

            products.add(product);
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }


   /**
    * This function break this url : "https://www.megamamute.com.br/lavadora-brastemp-15kg-30068859/p"
    * 
    * @param jsonSku
    * @param skuJson
    * @return
    */
   private String crawlName(JSONObject jsonSku, JSONObject skuJson) {
      String name = null;

      if (jsonSku.has("skuname")) {
         name = jsonSku.has("skuname") ? jsonSku.getString("skuname") : null;
      }

      return name;
   }

   private RatingsReviews crawlRating(Document document, String internalPid) {
      RatingsReviews ratingReviews = new RatingsReviews();

      ratingReviews.setDate(session.getDate());

      JSONObject trustVoxResponse = requestTrustVoxEndpoint(internalPid);

      Integer totalNumOfEvaluations = getTotalNumOfRatings(trustVoxResponse);
      Double totalRating = getTotalRating(trustVoxResponse);

      Double avgRating = null;
      if (totalNumOfEvaluations > 0) {
         avgRating = MathUtils.normalizeTwoDecimalPlaces(totalRating / totalNumOfEvaluations);
      }

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);

      return ratingReviews;
   }

   private Integer getTotalNumOfRatings(JSONObject trustVoxResponse) {
      if (trustVoxResponse.has("items")) {
         JSONArray ratings = trustVoxResponse.getJSONArray("items");
         return ratings.length();
      }
      return 0;
   }

   private Double getTotalRating(JSONObject trustVoxResponse) {
      Double totalRating = 0.0;
      if (trustVoxResponse.has("items")) {
         JSONArray ratings = trustVoxResponse.getJSONArray("items");

         for (int i = 0; i < ratings.length(); i++) {
            JSONObject rating = ratings.getJSONObject(i);

            if (rating.has("rate")) {
               totalRating += rating.getInt("rate");
            }
         }
      }
      return totalRating;
   }

   private JSONObject requestTrustVoxEndpoint(String id) {
      StringBuilder requestURL = new StringBuilder();

      requestURL.append("http://trustvox.com.br/widget/opinions?code=");
      requestURL.append(id);

      requestURL.append("&");
      requestURL.append("store_id=1355");

      requestURL.append("&");
      requestURL.append(session.getOriginalURL());

      Map<String, String> headerMap = new HashMap<>();
      headerMap.put(HttpHeaders.ACCEPT, "application/vnd.trustvox-v2+json");
      headerMap.put(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8");

      Request request = RequestBuilder.create().setUrl(requestURL.toString()).setCookies(cookies).setHeaders(headerMap).build();
      String response = this.dataFetcher.get(session, request).getBody();

      JSONObject trustVoxResponse;
      try {
         trustVoxResponse = new JSONObject(response);
      } catch (JSONException e) {
         Logging.printLogWarn(logger, session, "Error creating JSONObject from trustvox response.");
         Logging.printLogWarn(logger, session, CommonMethods.getStackTraceString(e));

         trustVoxResponse = new JSONObject();
      }

      return trustVoxResponse;
   }


   private boolean isProductPage(Document document) {
      return document.selectFirst(".product-detail") != null;
   }
}
