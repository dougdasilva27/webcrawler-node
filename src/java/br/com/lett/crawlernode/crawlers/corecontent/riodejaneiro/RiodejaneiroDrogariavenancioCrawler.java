package br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXCrawlersUtils;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.TrustvoxRatingCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

public class RiodejaneiroDrogariavenancioCrawler extends Crawler {
   private static final String HOME_PAGE = "https://www.drogariavenancio.com.br/";
   private static final String MAIN_SELLER_NAME_LOWER = "venancio produtos farmaceuticos ltda";

   public RiodejaneiroDrogariavenancioCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
   }


   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && href.startsWith(HOME_PAGE);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, MAIN_SELLER_NAME_LOWER, HOME_PAGE, cookies, dataFetcher);

         JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);
         String internalPid = vtexUtil.crawlInternalPid(skuJson);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".product__breadcrumb .bread-crumb ul li[typeof=\"v:Breadcrumb\"]", true);

         // sku data in json
         JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

         for (int i = 0; i < arraySkus.length(); i++) {
            JSONObject jsonSku = arraySkus.getJSONObject(i);
            String name = crawlName(skuJson); // because this site always show the principal name
            String internalId = vtexUtil.crawlInternalId(jsonSku);
            JSONObject apiJSON = vtexUtil.crawlApi(internalId);
            Map<String, Prices> marketplaceMap = vtexUtil.crawlMarketplace(apiJSON, internalId, false);
            Marketplace marketplace = vtexUtil.assembleMarketplaceFromMap(marketplaceMap);
            boolean available = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER);
            String primaryImage = vtexUtil.crawlPrimaryImage(apiJSON);
            String secondaryImages = vtexUtil.crawlSecondaryImages(apiJSON);
            String description = scrapDescription(vtexUtil, internalId);
            Prices prices = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER) ? marketplaceMap.get(MAIN_SELLER_NAME_LOWER) : new Prices();
            Float price = vtexUtil.crawlMainPagePrice(prices);
            Integer stock = vtexUtil.crawlStock(apiJSON);
            RatingsReviews ratingReviews = crawlRating(doc, internalId);
            List<String> eans = VTEXCrawlersUtils.scrapEanFromProductAPI(apiJSON);

            // Creating the product
            Product product = ProductBuilder.create().setUrl(session.getOriginalURL())
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
                  .setRatingReviews(ratingReviews)
                  .setMarketplace(marketplace)
                  .setEans(eans)
                  .build();

            products.add(product);
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private String crawlName(JSONObject skuJson) {
      String name = null;

      if (skuJson.has("name") && !skuJson.isNull("name")) {
         name = skuJson.getString("name");
      }

      return name;
   }

   private RatingsReviews crawlRating(Document doc, String id) {
      RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

      TrustvoxRatingCrawler r = new TrustvoxRatingCrawler(session, "105530", logger);
      ratingReviewsCollection = r.extractRatingAndReviewsForVtex(doc, dataFetcher);

      return ratingReviewsCollection.getRatingReviews(id);
   }

   private boolean isProductPage(Document document) {
      return document.select(".productName").first() != null;
   }

   public String crawlPrimaryImage(JSONObject json) {
      String primaryImage = null;

      if (json.has("Images")) {
         JSONArray jsonArrayImages = json.getJSONArray("Images");

         if (jsonArrayImages.length() > 0) {
            JSONArray arrayImage = jsonArrayImages.getJSONArray(0);
            JSONObject jsonImage = arrayImage.getJSONObject(0);

            if (jsonImage.has("Path")) {
               primaryImage = VTEXCrawlersUtils.changeImageSizeOnURL(jsonImage.getString("Path"));
            }
         }
      }

      return primaryImage;
   }

   public String crawlSecondaryImages(JSONObject apiInfo, String primaryImage) {
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      if (apiInfo.has("Images")) {
         JSONArray jsonArrayImages = apiInfo.getJSONArray("Images");

         for (int i = 0; i < jsonArrayImages.length(); i++) {
            JSONArray arrayImage = jsonArrayImages.getJSONArray(i);
            JSONObject jsonImage = arrayImage.getJSONObject(0);

            if (jsonImage.has("Path")) {
               String urlImage = VTEXCrawlersUtils.changeImageSizeOnURL(jsonImage.getString("Path"));

               if (!urlImage.equalsIgnoreCase(primaryImage)) {
                  secondaryImagesArray.put(urlImage);
               }
            }

         }
      }

      if (secondaryImagesArray.length() > 0) {
         secondaryImages = secondaryImagesArray.toString();
      }

      return secondaryImages;
   }

   private String scrapDescription(VTEXCrawlersUtils vtexUtil, String internalId) {
      StringBuilder descriptionBuilder = new StringBuilder();
      JSONObject obj = vtexUtil.crawlDescriptionAPI(internalId, "skuId");

      if (obj.has("Descrição")) {
         JSONArray arr = obj.getJSONArray("Descrição");

         if (arr.length() > 0) {
            descriptionBuilder.append("<div id=\"Descricao\">").append("<h4> Descrição </h4>");
            for (Object o : arr) {
               descriptionBuilder.append(o.toString());
            }
            descriptionBuilder.append("</div\">");
         }
      }

      /**
       * Because of this link: https://www.drogariavenancio.com.br/fibermais10sachesx5g-8198/p
       */

      if (obj.has("Indicações")) {
         JSONArray arr = obj.getJSONArray("Indicações");

         if (arr.length() > 0) {
            descriptionBuilder.append("<div id=\"Indicacoes\">").append("<h4> Indicações </h4>");
            for (Object o : arr) {
               descriptionBuilder.append(o.toString());
            }
            descriptionBuilder.append("</div\">");
         }
      }


      if (obj.has("Contra indicações")) {
         JSONArray arr = obj.getJSONArray("Contra indicações");

         if (arr.length() > 0) {
            descriptionBuilder.append("<div id=\"contra indicacoes\">").append("<h4> Contra indicações </h4>");
            for (Object o : arr) {
               descriptionBuilder.append(o.toString());
            }
            descriptionBuilder.append("</div\">");
         }
      }

      descriptionBuilder.append(CrawlerUtils.scrapLettHtml(internalId, session, session.getMarket().getNumber()));

      return descriptionBuilder.toString();
   }
}
