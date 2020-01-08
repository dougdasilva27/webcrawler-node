package br.com.lett.crawlernode.crawlers.corecontent.peru;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXCrawlersUtils;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

public class PeruMetroCrawler extends Crawler {

   public PeruMetroCrawler(Session session) {
      super(session);
   }

   private static final String HOME_PAGE = "https://www.metro.pe/";
   private static final String MAIN_SELLER_NAME_LOWER = "wong";
   private static final String MAIN_SELLER_NAME_LOWER_2 = "metro";

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
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
         String url = "https://www.metro.pe/wongfood/dataentities/RE/documents/" + internalPid + "?_fields=reviews";
         Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
         Document html = Jsoup.parse(this.dataFetcher.get(session, request).getBody());
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".bread-crumb li:not(:first-child) > a");
         String description = CrawlerUtils.scrapSimpleDescription(doc,
               Arrays.asList(".productDescription", ".title[data-destec=\"descp\"]", ".content-description .value-field.Descripcion",
                     ".title[data-destec=\"tech\"]", ".content-description table.group.Especificaciones-Tecnicas"));

         // sku data in json
         JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();
         JSONArray eanArray = CrawlerUtils.scrapEanFromVTEX(doc);

         for (int i = 0; i < arraySkus.length(); i++) {
            JSONObject jsonSku = arraySkus.getJSONObject(i);
            String internalId = vtexUtil.crawlInternalId(jsonSku);
            JSONObject apiJSON = vtexUtil.crawlApi(internalId);
            String name = vtexUtil.crawlName(jsonSku, skuJson, " ");
            Map<String, Prices> marketplaceMap = vtexUtil.crawlMarketplace(apiJSON, internalId, false);
            List<String> metroSellers = CrawlerUtils.getMainSellers(marketplaceMap, Arrays.asList(MAIN_SELLER_NAME_LOWER, MAIN_SELLER_NAME_LOWER_2));
            Marketplace marketplace = CrawlerUtils.assembleMarketplaceFromMap(marketplaceMap, metroSellers, Arrays.asList(Card.VISA), session);
            boolean available = CrawlerUtils.getAvailabilityFromMarketplaceMap(marketplaceMap, metroSellers);
            String primaryImage = vtexUtil.crawlPrimaryImage(apiJSON);
            String secondaryImages = vtexUtil.crawlSecondaryImages(apiJSON);
            Prices prices = CrawlerUtils.getPrices(marketplaceMap, metroSellers);
            Float price = CrawlerUtils.extractPriceFromPrices(prices, Card.VISA);
            Integer stock = vtexUtil.crawlStock(apiJSON);
            String ean = i < eanArray.length() ? eanArray.getString(i) : null;
            RatingsReviews ratingReviews = extractReviews(html, internalId);

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

   private RatingsReviews extractReviews(Document doc, String internalId) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalNumOfEvaluations = 0;
      Double avgRating = 0.0;

      Element aux = doc.selectFirst("body");

      if (aux != null && !aux.text().isEmpty()) {
         JSONObject outterJson = new JSONObject(aux.text());

         if (outterJson.has("reviews")) {
            JSONObject middleJson = new JSONObject(outterJson.get("reviews").toString());

            for (String s : middleJson.keySet()) {
               JSONObject innerJson = middleJson.getJSONObject(s);

               // Valid review
               if (!innerJson.has("date")) {
                  continue;
               }

               totalNumOfEvaluations += 1;

               if (innerJson.has("rating")) {
                  avgRating = (double) (avgRating + innerJson.getInt("rating"));;
               }
            }

            // Handling division by 0
            if (totalNumOfEvaluations != 0) {
               avgRating /= totalNumOfEvaluations;
            }
         }
      }

      ratingReviews.setInternalId(internalId);
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);

      return ratingReviews;
   }

}
