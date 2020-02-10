package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXCrawlersUtils;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.YourreviewsRatingCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

public class BrasilNovomundoCrawler extends Crawler {

   private static final String HOME_PAGE = "http://www.novomundo.com.br/";
   private static final String MAIN_SELLER_NAME_LOWER = "novo mundo";

   public BrasilNovomundoCrawler(Session session) {
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
         VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, MAIN_SELLER_NAME_LOWER, HOME_PAGE, cookies, dataFetcher);

         JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);

         String internalPid = vtexUtil.crawlInternalPid(skuJson);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".bread-crumb li a");
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".product-menu .productDescription", "#caracteristicas"));

         Map<String, Integer> productsDiscount = scrapSkusDiscount(internalPid);

         // sku data in json
         JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

         // ean data in json
         JSONArray arrayEans = CrawlerUtils.scrapEanFromVTEX(doc);

         for (int i = 0; i < arraySkus.length(); i++) {
            JSONObject jsonSku = arraySkus.getJSONObject(i);
            String internalId = vtexUtil.crawlInternalId(jsonSku);
            Integer discount = productsDiscount.containsKey(internalId) ? productsDiscount.get(internalId) : 0;
            vtexUtil.setBankTicketDiscount(discount);
            vtexUtil.setCardDiscount(discount);

            JSONObject apiJSON = vtexUtil.crawlApi(internalId);
            String name = vtexUtil.crawlName(jsonSku, skuJson, " ");
            Map<String, Prices> marketplaceMap = vtexUtil.crawlMarketplace(apiJSON, internalId, true);
            Marketplace marketplace = vtexUtil.assembleMarketplaceFromMap(marketplaceMap);
            boolean available = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER);
            String primaryImage = vtexUtil.crawlPrimaryImage(apiJSON);
            String secondaryImages = vtexUtil.crawlSecondaryImages(apiJSON);
            Prices prices = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER) ? marketplaceMap.get(MAIN_SELLER_NAME_LOWER) : new Prices();
            Float price = vtexUtil.crawlMainPagePrice(prices);
            Integer stock = vtexUtil.crawlStock(apiJSON);
            String ean = i < arrayEans.length() ? arrayEans.getString(i) : null;
            RatingsReviews ratingReviews = crawlRating(doc, internalPid);

            List<String> eans = new ArrayList<>();
            eans.add(ean);

            // Creating the product
            Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
                  .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
                  .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
                  .setStock(stock).setMarketplace(marketplace).setRatingReviews(ratingReviews).setEans(eans).build();

            products.add(product);
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }



   /*******************************
    * Product page identification *
    *******************************/

   private boolean isProductPage(Document document) {
      return document.selectFirst(".productName") != null;
   }

   private RatingsReviews crawlRating(Document doc, String internalPid) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      YourreviewsRatingCrawler yrRC = new YourreviewsRatingCrawler(session, cookies, logger);
      Document docRating = yrRC.crawlPageRatingsFromYourViews(internalPid, "4c93a458-0ff1-453e-b5b6-b361ad6aaeda", dataFetcher);

      Integer totalNumOfEvaluations = yrRC.getTotalNumOfRatingsFromYourViews(docRating);
      Double avgRating = yrRC.getTotalAvgRatingFromYourViews(docRating);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);

      return ratingReviews;
   }


   private Map<String, Integer> scrapSkusDiscount(String internalPid) {
      Map<String, Integer> skuDiscount = new HashMap<>();

      String payload = "{\"apiKey\":\"novomundo\",\"page\":{\"name\":\"product\",\"url\":\"" + session.getOriginalURL() + "\"},\"source\":\"desktop\","
            + "\"referenceProduct\":{\"id\":\"" + internalPid + "\"}}";
      StringBuilder urlApi = new StringBuilder();
      urlApi.append("https://onsite.chaordicsystems.com/v5/recommend/all?callback=jQuery171005651111547091481_1555104591808&q=");
      try {
         urlApi.append(URLEncoder.encode(payload, "UTF-8"));
      } catch (UnsupportedEncodingException e) {
         Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
      }

      String response = this.dataFetcher.get(session, RequestBuilder.create().setUrl(urlApi.toString()).setCookies(cookies).build()).getBody();
      JSONObject chaordicJson = CrawlerUtils.stringToJson(CrawlerUtils.extractSpecificStringFromScript(response, "(", ");", true));

      if (chaordicJson.has("widgets")) {
         JSONObject widgets = chaordicJson.getJSONObject("widgets");

         for (String key : widgets.keySet()) {
            JSONObject sessionJson = widgets.getJSONObject(key);

            if (sessionJson.has("result")) {
               JSONObject result = sessionJson.getJSONObject("result");

               if (result.has("displays") && !result.isNull("displays")) {
                  JSONArray displays = result.getJSONArray("displays");

                  if (displays.length() > 0) {
                     JSONObject display = displays.getJSONObject(0);

                     if (display.has("refs") && !display.isNull("refs")) {
                        JSONArray refs = display.getJSONArray("refs");

                        if (refs.length() > 0) {
                           JSONObject ref = refs.getJSONObject(0);

                           JSONArray arraySkus = ref.has("skus") ? ref.getJSONArray("skus") : new JSONArray();
                           for (int i = 0; i < arraySkus.length(); i++) {
                              JSONObject jsonSku = arraySkus.getJSONObject(i);

                              String id = jsonSku.has("sku") ? jsonSku.get("sku").toString() : null;
                              if (id != null && jsonSku.has("details")) {
                                 JSONObject details = jsonSku.getJSONObject("details");

                                 if (details.has("discount")) {
                                    JSONObject discount = details.getJSONObject("discount");

                                    skuDiscount.put(id, CrawlerUtils.getIntegerValueFromJSON(discount, "percent", 0));
                                 }
                              }
                           }

                           break;
                        }
                     }
                  }
               }
            }
         }
      }

      return skuDiscount;
   }
}
