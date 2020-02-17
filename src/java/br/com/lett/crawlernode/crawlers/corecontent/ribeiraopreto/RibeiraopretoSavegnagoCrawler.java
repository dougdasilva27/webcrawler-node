package br.com.lett.crawlernode.crawlers.corecontent.ribeiraopreto;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.Card;
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

public class RibeiraopretoSavegnagoCrawler extends Crawler {

   /*
    * Ribeirão Preto - 1 Sertãozinho - 6 Jardinópolis - 11 Jaboticabal - 7 Franca - 3 Barretos - 10
    * Bebedouro - 9 Monte Alto - 12 Araraquara - 4 São carlos - 5 Matão - 8
    */

   private static final String HOME_PAGE = "https://www.savegnago.com.br/";
   private static final String MAIN_SELLER_NAME_LOWER = "savegnago supermercados";

   public RibeiraopretoSavegnagoCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   public String handleURLBeforeFetch(String curURL) {

      if (curURL.endsWith("/p")) {
         try {
            String url = curURL;
            List<NameValuePair> paramsOriginal = URLEncodedUtils.parse(new URI(url), "UTF-8");
            List<NameValuePair> paramsNew = new ArrayList<>();

            for (NameValuePair param : paramsOriginal) {
               if (!param.getName().equals("sc")) {
                  paramsNew.add(param);
               }
            }

            paramsNew.add(new BasicNameValuePair("sc", "2"));
            URIBuilder builder = new URIBuilder(curURL.split("\\?")[0]);

            builder.clearParameters();
            builder.setParameters(paramsNew);

            return builder.build().toString();

         } catch (URISyntaxException e) {
            return curURL;
         }
      }

      return curURL;

   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, MAIN_SELLER_NAME_LOWER, HOME_PAGE, cookies, dataFetcher);
         vtexUtil.setHasBankTicket(false);

         JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);
         String internalPid = vtexUtil.crawlInternalPid(skuJson);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".bread-crumb > ul li a");

         // sku data in json
         JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

         // ean data in json
         JSONArray arrayEans = CrawlerUtils.scrapEanFromVTEX(doc);

         for (int i = 0; i < arraySkus.length(); i++) {
            JSONObject jsonSku = arraySkus.getJSONObject(i);


            String internalId = vtexUtil.crawlInternalId(jsonSku);
            JSONObject apiJSON = vtexUtil.crawlApi(internalId, "?sc=2");
            String name = vtexUtil.crawlName(jsonSku, skuJson, " ");
            String description = scrapDescription(doc, internalId);
            Map<String, Prices> marketplaceMap = vtexUtil.crawlMarketplace(apiJSON, internalId, false);
            Marketplace marketplace = CrawlerUtils.assembleMarketplaceFromMap(marketplaceMap, Arrays.asList(MAIN_SELLER_NAME_LOWER), Card.VISA, session);
            boolean available = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER);
            String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "div .image-zoom", Arrays.asList("href"), "https:", "www.savegnago.com.br/");
            String secondaryImages = vtexUtil.crawlSecondaryImages(apiJSON);
            Prices prices = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER) ? marketplaceMap.get(MAIN_SELLER_NAME_LOWER) : new Prices();
            Float price = vtexUtil.crawlMainPagePrice(prices);
            Integer stock = vtexUtil.crawlStock(apiJSON);
            String ean = i < arrayEans.length() ? arrayEans.getString(i) : null;
            RatingsReviews ratingReviews = crawlRatingReviews(internalId);

            List<String> eans = new ArrayList<>();
            eans.add(ean);

            // Creating the product
            Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
                  .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
                  .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
                  .setStock(stock).setMarketplace(marketplace).setEans(eans).setRatingReviews(ratingReviews).build();

            products.add(product);
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }


   private boolean isProductPage(Document document) {
      return document.selectFirst(".productName") != null;
   }

   private RatingsReviews crawlRatingReviews(String internalId) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      YourreviewsRatingCrawler yr = new YourreviewsRatingCrawler(session, cookies, logger, "d23c4a07-61d5-43d3-97da-32c0680a32b8", dataFetcher);
      Document docRating = yr.crawlPageRatingsFromYourViews(internalId, "d23c4a07-61d5-43d3-97da-32c0680a32b8", dataFetcher);

      Integer totalNumOfEvaluations = yr.getTotalNumOfRatingsFromYourViews(docRating);
      Double avgRating = yr.getTotalAvgRatingFromYourViews(docRating);
      AdvancedRatingReview advancedRatingReview = yr.getTotalStarsFromEachValue(internalId);

      ratingReviews.setAdvancedRatingReview(advancedRatingReview);
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);

      return ratingReviews;

   }

   private String scrapDescription(Document doc, String internalId) {
      StringBuilder description = new StringBuilder();

      description.append(CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".productDescription")));
      description.append(CrawlerUtils.scrapLettHtml(internalId, session, session.getMarket().getNumber()));

      return description.toString();
   }

}
