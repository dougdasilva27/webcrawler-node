package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import models.RatingsReviews;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
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
import models.prices.Prices;


public class BrasilFarmaciaindianaCrawler extends Crawler {

   private static final String HOME_PAGE = "http://www.farmaciaindiana.com.br/";
   private static final String MAIN_SELLER_NAME_LOWER = "farmácia indiana";
   private static final String MAIN_SELLER_STORE_ID = "110688";

   public BrasilFarmaciaindianaCrawler(Session session) {
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

         JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);

         String internalPid = vtexUtil.crawlInternalPid(skuJson);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".bread-crumb > ul li:not(:first-child) a");
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".productDescription", "#caracteristicas"));
         JSONArray eanArray = CrawlerUtils.scrapEanFromVTEX(doc);
         RatingsReviews ratingReviews = scrapRatingReviews(internalPid, doc);

         // sku data in json
         JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

         for (int i = 0; i < arraySkus.length(); i++) {
            JSONObject jsonSku = arraySkus.getJSONObject(i);

            String internalId = vtexUtil.crawlInternalId(jsonSku);
            JSONObject apiJSON = vtexUtil.crawlApi(internalId);
            String name = vtexUtil.crawlName(jsonSku, skuJson);
            Map<String, Prices> marketplaceMap = vtexUtil.crawlMarketplace(apiJSON, internalId, true);
            Marketplace marketplace = vtexUtil.assembleMarketplaceFromMap(marketplaceMap);
            boolean available = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER);
            String primaryImage = vtexUtil.crawlPrimaryImage(apiJSON);
            String secondaryImages = vtexUtil.crawlSecondaryImages(apiJSON);
            Prices prices = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER) ? marketplaceMap.get(MAIN_SELLER_NAME_LOWER) : new Prices();
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

   private boolean isProductPage(Document document) {
      return document.selectFirst(".productName") != null;
   }

   private RatingsReviews scrapRatingReviews(String internalId, Document doc) {
      TrustvoxRatingCrawler trustVox = new TrustvoxRatingCrawler(session, MAIN_SELLER_STORE_ID, logger);
      return trustVox.extractRatingAndReviews(internalId, doc, dataFetcher);
   }

}
