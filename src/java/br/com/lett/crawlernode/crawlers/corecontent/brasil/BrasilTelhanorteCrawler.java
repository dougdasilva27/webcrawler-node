package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.TrustvoxRatingCrawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXCrawlersUtils;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

public class BrasilTelhanorteCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.telhanorte.com.br/";
   private static final String MAIN_SELLER_NAME_LOWER = "telhanorte";

   public BrasilTelhanorteCrawler(Session session) {
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
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".bread-crumb > ul > :not(:first-child) > a");

         // sku data in json
         JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();
         JSONArray arrayEan = CrawlerUtils.scrapEanFromVTEX(doc);

         for (int i = 0; i < arraySkus.length(); i++) {
            JSONObject jsonSku = arraySkus.getJSONObject(i);

            String internalId = vtexUtil.crawlInternalId(jsonSku);
            JSONObject apiJSON = vtexUtil.crawlApi(internalId);
            String name = vtexUtil.crawlName(jsonSku, skuJson, "");
            String description = crawlDescription(vtexUtil, internalId);
            Map<String, Prices> marketplaceMap = vtexUtil.crawlMarketplace(apiJSON, internalId, false);
            Marketplace marketplace = vtexUtil.assembleMarketplaceFromMap(marketplaceMap);
            boolean available = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER);
            String primaryImage = vtexUtil.crawlPrimaryImage(apiJSON);
            String secondaryImages = vtexUtil.crawlSecondaryImages(apiJSON);
            Prices prices = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER) ? marketplaceMap.get(MAIN_SELLER_NAME_LOWER) : new Prices();
            Float price = vtexUtil.crawlMainPagePrice(prices);
            Integer stock = vtexUtil.crawlStock(apiJSON);
            RatingsReviews ratingReviews = scrapRating(internalId, doc);


            String ean = i < arrayEan.length() ? arrayEan.getString(i) : null;

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


   private boolean isProductPage(Document document) {
      return document.selectFirst(".m-product") != null;
   }


   private String crawlDescription(VTEXCrawlersUtils vtexUtil, String internalId) {
      StringBuilder description = new StringBuilder();

      JSONObject descriptionApi = vtexUtil.crawlDescriptionAPI(internalId, "skuId");
      List<String> specs = new ArrayList<>();

      if (descriptionApi.has("allSpecifications")) {
         JSONArray keys = descriptionApi.getJSONArray("allSpecifications");
         for (Object o : keys) {
            if (!o.toString().equalsIgnoreCase("Informações para Instalação") && !o.toString().equalsIgnoreCase("Portfólio")) {
               specs.add(o.toString());
            }
         }
      }

      for (String spec : specs) {
         description.append("<div>");
         description.append("<h4>").append(spec).append("</h4>");
         description.append(VTEXCrawlersUtils.sanitizeDescription(descriptionApi.get(spec)));
         description.append("</div>");
      }

      return description.toString();
   }

   private RatingsReviews scrapRating(String internalId, Document doc) {
      TrustvoxRatingCrawler trustVox = new TrustvoxRatingCrawler(session, "73909", logger);
      return trustVox.extractRatingAndReviews(internalId, doc, dataFetcher);
   }


}
