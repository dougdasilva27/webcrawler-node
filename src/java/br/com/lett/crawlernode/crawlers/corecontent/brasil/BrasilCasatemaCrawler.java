package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXCrawlersUtils;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.YourreviewsRatingCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.AdvancedRatingReview;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

public class BrasilCasatemaCrawler extends Crawler {

   public BrasilCasatemaCrawler(Session session) {
      super(session);
   }

   private static final String HOME_PAGE = "https://www.casatema.com.br/";
   private static final String MAIN_SELLER_NAME_LOWER = "casatema";

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<Product>();

      if (isProductPage(doc)) {
         VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, MAIN_SELLER_NAME_LOWER, HOME_PAGE, cookies, dataFetcher);

         JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);
         String internalPid = vtexUtil.crawlInternalPid(skuJson);

         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".bread-crumb li > a", true);
         String description = CrawlerUtils.scrapSimpleDescription(doc,
               Arrays.asList(".productDescription", "#caracteristicas"));

         // sku data in json
         JSONArray arraySkus =
               skuJson != null &&
                     skuJson.has("skus") &&
                     !skuJson.isNull("skus")
                           ? skuJson.getJSONArray("skus")
                           : new JSONArray();

         for (int i = 0; i < arraySkus.length(); i++) {
            JSONObject jsonSku = arraySkus.getJSONObject(i);

            String internalId = vtexUtil.crawlInternalId(jsonSku);
            JSONObject apiJSON = vtexUtil.crawlApi(internalId);
            String name = vtexUtil.crawlName(jsonSku, skuJson, " ");
            String primaryImage = vtexUtil.crawlPrimaryImage(apiJSON);
            String secondaryImages = vtexUtil.crawlSecondaryImages(apiJSON);
            Map<String, Prices> marketplaceMap = vtexUtil.crawlMarketplace(apiJSON, internalId, false);
            Marketplace marketplace = vtexUtil.assembleMarketplaceFromMap(marketplaceMap);
            boolean available = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER);
            Prices prices = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER) ? marketplaceMap.get(MAIN_SELLER_NAME_LOWER) : new Prices();
            Float price = vtexUtil.crawlMainPagePrice(prices);
            Integer stock = vtexUtil.crawlStock(apiJSON);
            JSONArray eanArray = CrawlerUtils.scrapEanFromVTEX(doc);

            String ean = i < eanArray.length() ? eanArray.getString(i) : null;
            RatingsReviews ratingReviews = crawRating(internalPid);
            List<String> eans = new ArrayList<>();
            eans.add(ean);

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
                  .setStock(stock)
                  .setMarketplace(marketplace)
                  .setEans(eans)
                  .setRatingReviews(ratingReviews)
                  .build();

            products.add(product);
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }
      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".produto") != null;
   }

   private RatingsReviews crawRating(String internalPid) {
      YourreviewsRatingCrawler yourReviews = new YourreviewsRatingCrawler(session, cookies, logger, "14d4153f-8723-4067-8f11-24d75baf7406", this.dataFetcher);

      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Document docRating = yourReviews.crawlPageRatingsFromYourViews(internalPid, "14d4153f-8723-4067-8f11-24d75baf7406", dataFetcher);
      Integer totalNumOfEvaluations = yourReviews.getTotalNumOfRatingsFromYourViews(docRating);
      Double avgRating = yourReviews.getTotalAvgRatingFromYourViews(docRating);
      AdvancedRatingReview advancedRatingReview = yourReviews.getTotalStarsFromEachValue(internalPid);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);

      return ratingReviews;
   }

   public Document crawlAllPagesRatingsFromYourViews(String internalPid, String storeKey, DataFetcher dataFetcher, Integer currentPage) {
      Document doc = new Document("");

      String url = "https://service.yourviews.com.br/review/GetReview?storeKey=" + storeKey + "&productStoreId=" + internalPid + "&extFilters=&page="
            + currentPage + "&callback=_jqjsp&";

      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
      String response = dataFetcher.get(session, request).getBody().trim();

      if (response.startsWith("<")) {
         doc = Jsoup.parse(response);
      } else if (response.contains("({")) {
         int x = response.indexOf("({") + 1;
         int y = response.lastIndexOf("})");

         String responseJson = response.substring(x, y + 1).trim();
         JSONObject json = CrawlerUtils.stringToJson(responseJson);

         if (json.has("html")) {
            doc = Jsoup.parse(json.get("html").toString());
         } else {
            doc = Jsoup.parse(response);
         }
      }

      return doc;
   }

}
