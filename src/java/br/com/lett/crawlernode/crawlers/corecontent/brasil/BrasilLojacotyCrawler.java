package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.Card;
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

public class BrasilLojacotyCrawler extends Crawler {
   public BrasilLojacotyCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
   }

   private static final String HOME_PAGE = "https://www.lojacoty.com.br/";
   private static final String MAIN_SELLER_NAME_LOWER = "coty";
   private static final String MAIN_SELLER_NAME_LOWER_2 = "lojacoty";

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
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".bread-crumb li:not(:first-child) > a");
         // sku data in json
         JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

         JSONArray eanArray = CrawlerUtils.scrapEanFromVTEX(doc);

         for (int i = 0; i < arraySkus.length(); i++) {
            JSONObject jsonSku = arraySkus.getJSONObject(i);

            String internalId = vtexUtil.crawlInternalId(jsonSku);
            String description = crawlDescription(internalId, vtexUtil);
            JSONObject apiJSON = vtexUtil.crawlApi(internalId);
            String name = vtexUtil.crawlName(jsonSku, skuJson);
            Map<String, Prices> marketplaceMap = vtexUtil.crawlMarketplace(apiJSON, internalId, true);
            Marketplace marketplace = CrawlerUtils.assembleMarketplaceFromMap(marketplaceMap,
                  Arrays.asList(MAIN_SELLER_NAME_LOWER, MAIN_SELLER_NAME_LOWER_2), Card.VISA, session);
            boolean available = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER) || marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER_2);
            String primaryImage = vtexUtil.crawlPrimaryImage(apiJSON);
            String secondaryImages = vtexUtil.crawlSecondaryImages(apiJSON);
            Prices prices = CrawlerUtils.getPrices(marketplaceMap, Arrays.asList(MAIN_SELLER_NAME_LOWER, MAIN_SELLER_NAME_LOWER_2));
            Float price = vtexUtil.crawlMainPagePrice(prices);
            Integer stock = vtexUtil.crawlStock(apiJSON);
            String ean = i < eanArray.length() ? eanArray.getString(i) : null;
            RatingsReviews ratingReviews = scrapRating(internalId, doc);

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


   private boolean isProductPage(Document document) {
      return document.selectFirst(".productName") != null;
   }

   private RatingsReviews scrapRating(String internalId, Document doc) {
      TrustvoxRatingCrawler trustVox = new TrustvoxRatingCrawler(session, "107587", logger);
      return trustVox.extractRatingAndReviews(internalId, doc, dataFetcher);
   }

   private String crawlDescription(String internalId, VTEXCrawlersUtils vtexUtil) {
      StringBuilder description = new StringBuilder();

      JSONObject descriptionJson = vtexUtil.crawlDescriptionAPI(internalId, "skuId");

      if (descriptionJson.has("description")) {
         description.append("<div><h4>Descrição</h4>");

         if (descriptionJson.has("Título da Descrição")) {
            description.append(descriptionJson.get("Título da Descrição").toString().replace("[\"", "").replace("\"]", ""));
         }
         description.append(sanitizeDescription(descriptionJson.get("description")));
         description.append("</div>");
      }

      if (descriptionJson.has("Cor")) {
         description.append("<div><h4>Cor</h4>");
         description.append(sanitizeDescription(descriptionJson.get("Cor")));
         description.append("</div>");
      }

      if (descriptionJson.has("Composição")) {
         description.append("<div><h4>Composição</h4>");
         description.append(sanitizeDescription(descriptionJson.get("Composição")));
         description.append("</div>");
      }

      if (descriptionJson.has("Gênero")) {
         description.append("<div><h4>Gênero</h4>");
         description.append(sanitizeDescription(descriptionJson.get("Gênero")));
         description.append("</div>");
      }

      if (descriptionJson.has("Modo de Uso")) {
         description.append("<div><h4>Modo de Usar</h4>");
         description.append(sanitizeDescription(descriptionJson.get("Modo de Uso")));
         description.append("</div>");
      }

      if (descriptionJson.has("Instruções de Uso")) {
         description.append("<div><h4>Instruções de Uso</h4>");
         description.append(sanitizeDescription(descriptionJson.get("Instruções de Uso")));
         description.append("</div>");
      }

      return description.toString();
   }

   private Document sanitizeDescription(Object obj) {
      return Jsoup.parse(obj.toString().replace("[\"", "").replace("\"]", "").replace("\\", ""));
   }

}
