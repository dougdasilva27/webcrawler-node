package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.TrustvoxRatingCrawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXCrawlersUtils;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static br.com.lett.crawlernode.util.CrawlerUtils.crawlSkuJsonVTEX;


public class BrasilFarmaciaindianaCrawler extends Crawler {

   private static final String HOME_PAGE = "http://www.farmaciaindiana.com.br/";
   private static final String SELLER_NAME = "farmácia indiana";
   private static final String MAIN_SELLER_STORE_ID = "110688";
   private static final String VTEX_SEGMENT = "vtex_segment=eyJjYW1wYWlnbnMiOm51bGwsImNoYW5uZWwiOiIxIiwicHJpY2VUYWJsZXMiOm51bGwsInJlZ2lvbklkIjpudWxsLCJ1dG1fY2FtcGFpZ24iOm51bGwsInV0bV9zb3VyY2UiOm51bGwsInV0bWlfY2FtcGFpZ24iOm51bGwsImN1cnJlbmN5Q29kZSI6IkJSTCIsImN1cnJlbmN5U3ltYm9sIjoiUiQiLCJjb3VudHJ5Q29kZSI6IkJSQSIsImN1bHR1cmVJbmZvIjoicHQtQlIiLCJjaGFubmVsUHJpdmFjeSI6InB1YmxpYyJ9";

   public BrasilFarmaciaindianaCrawler(Session session) {
      super(session);
   }

   protected String getHomePage() {
      return HOME_PAGE;
   }

   protected List<String> getMainSellersNames() {
      return Collections.singletonList(SELLER_NAME);
   }

   @Override
   public void handleCookiesBeforeFetch() {
      BasicClientCookie cookie = new BasicClientCookie("vtex_segment", VTEX_SEGMENT);
      cookie.setDomain(HOME_PAGE);
      cookie.setPath("/");
      cookies.add(cookie);
   }

   @Override
   protected Object fetch() {
      Request request = Request.RequestBuilder.create()
         .setUrl(session.getOriginalURL())
         .setCookies(cookies)
         .build();

      return Jsoup.parse(dataFetcher.get(session, request).getBody());
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, SELLER_NAME, HOME_PAGE, cookies, dataFetcher);

         JSONObject skuJson = crawlSkuJsonVTEX(doc, session);

         String internalPid = vtexUtil.crawlInternalPid(skuJson);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".bread-crumb > ul li:not(:first-child) a");
         String description = scrapDescription(doc);
         JSONArray eanArray = CrawlerUtils.scrapEanFromVTEX(doc);
         RatingsReviews ratingReviews = scrapRating(internalPid, null, doc, null);

         // sku data in json
         JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

         for (int i = 0; i < arraySkus.length(); i++) {
            JSONObject jsonSku = arraySkus.getJSONObject(i);

            String internalId = vtexUtil.crawlInternalId(jsonSku);
            JSONObject apiJSON = vtexUtil.crawlApi(internalId);
            String name = scrapName(doc, vtexUtil, jsonSku, skuJson);
            Map<String, Prices> marketplaceMap = vtexUtil.crawlMarketplace(apiJSON, internalId, true);
            Marketplace marketplace = vtexUtil.assembleMarketplaceFromMap(marketplaceMap);
            boolean available = marketplaceMap.containsKey(SELLER_NAME);
            String primaryImage = vtexUtil.crawlPrimaryImage(apiJSON);
            String secondaryImages = vtexUtil.crawlSecondaryImages(apiJSON);
            Prices prices = marketplaceMap.containsKey(SELLER_NAME) ? marketplaceMap.get(SELLER_NAME) : new Prices();
            Float price = vtexUtil.crawlMainPagePrice(prices);
            Integer stock = vtexUtil.crawlStock(apiJSON);
            String ean = i < eanArray.length() ? eanArray.getString(i) : null;

            List<String> productEANs = new ArrayList<>();
            productEANs.add(ean);

            // Creating the product
            Product product = ProductBuilder
               .create()
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
               .setEans(productEANs)
               .setRatingReviews(ratingReviews)
               .build();

            products.add(product);
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }


   /**
    * There are some product pages that do not have a name
    * https://www.farmaciaindiana.com.br/100--albumina-health-labs-natural-500g/p
    */
   private String scrapName(Document doc, VTEXCrawlersUtils vtexUtil, JSONObject jsonSku, JSONObject skuJson) {
      boolean hasTitle = !doc.select(".hidden .plugin-preco").isEmpty();
      String name = vtexUtil.crawlName(jsonSku, skuJson);
      if (!hasTitle) {
         name = " ";
      }

      return name;
   }

   private String scrapDescription(Document doc) {
      StringBuilder description = new StringBuilder();
      String characteristics = CrawlerUtils.scrapStringSimpleInfo(doc, "#caracteristicas", true);
      String productDescription = doc.select(".bf-description__container .productDescription p").text();

      if (productDescription != null && !productDescription.isEmpty()) {
         description.append("Descrição").append("\n");
         description.append(productDescription).append("\n");
      }
      if (characteristics != null && !characteristics.isEmpty()) {
         description.append("Características").append("\n");

         description.append(characteristics);
      }

      return description.toString();

   }

   protected boolean isProductPage(Document document) {
      return document.selectFirst(".productName") != null;
   }

   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      TrustvoxRatingCrawler trustVox = new TrustvoxRatingCrawler(session, MAIN_SELLER_STORE_ID, logger);
      JSONObject json = crawlSkuJsonVTEX(doc, session);
      String id = json.optString("productId");
      if (id != null && !id.isEmpty()) {
         return trustVox.extractRatingAndReviews(id, doc, new FetcherDataFetcher());
      } else {
         return trustVox.extractRatingAndReviews(internalPid, doc, new FetcherDataFetcher());
      }
   }

}
