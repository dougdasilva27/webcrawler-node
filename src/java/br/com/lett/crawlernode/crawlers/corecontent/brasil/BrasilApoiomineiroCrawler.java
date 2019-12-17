package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
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
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.Pair;
import models.Marketplace;
import models.prices.Prices;

public class BrasilApoiomineiroCrawler extends Crawler {

   public BrasilApoiomineiroCrawler(Session session) {
      super(session);
   }

   private static final String HOME_PAGE = "http://www.apoioentrega.com/";
   private static final String HOME_PAGE_HTTPS = "https://www.apoioentrega.com/";
   private static final String MAIN_SELLER_NAME_LOWER = "apoio entrega";
   private static final String PACKAGE_API_URL = "https://www2.apoioentrega.com.br/api-vtex.php";

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE) || href.startsWith(HOME_PAGE_HTTPS));
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
         String description = crawlDescription(doc);
         String primaryImage = null;
         String secondaryImages = null;

         // sku data in json
         JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

         // ean data in json
         JSONArray arrayEans = CrawlerUtils.scrapEanFromVTEX(doc);

         for (int i = 0; i < arraySkus.length(); i++) {
            JSONObject jsonSku = arraySkus.getJSONObject(i);

            String internalId = vtexUtil.crawlInternalId(jsonSku);
            JSONObject apiJSON = vtexUtil.crawlApi(internalId);
            String name = apiJSON.has("Name") ? apiJSON.get("Name").toString() : vtexUtil.crawlName(jsonSku, skuJson, " ");
            Map<String, Prices> marketplaceMap = vtexUtil.crawlMarketplace(apiJSON, internalId, true);
            Marketplace marketplace = vtexUtil.assembleMarketplaceFromMap(marketplaceMap);
            boolean available = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER);
            primaryImage = vtexUtil.crawlPrimaryImage(apiJSON);
            secondaryImages = vtexUtil.crawlSecondaryImages(apiJSON);
            Prices prices = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER) ? marketplaceMap.get(MAIN_SELLER_NAME_LOWER) : new Prices();
            Float price = vtexUtil.crawlMainPagePrice(prices);
            Integer stock = vtexUtil.crawlStock(apiJSON);
            String skuDescription = description + CrawlerUtils.scrapLettHtml(internalId, session, session.getMarket().getNumber());
            String ean = i < arrayEans.length() ? arrayEans.getString(i) : null;

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
                  .setDescription(skuDescription)
                  .setStock(stock)
                  .setMarketplace(marketplace)
                  .setEans(eans)
                  .build();

            List<Pair<Double, Integer>> quantities = fetchQuantities(internalId);
            if (!quantities.isEmpty()) {
               for (Pair<Double, Integer> pack : quantities) {
                  Product clone = product.clone();

                  clone.setInternalId(product.getInternalId() + "-" + pack.getSecond());
                  clone.setName(product.getName() + " - " + pack.getSecond() + " un");

                  Float clonePrice = pack.getFirst().floatValue() * pack.getSecond();

                  clone.setPrice(clonePrice);
                  clone.setPrices(buildPackPrices(clonePrice));

                  products.add(clone);
               }
            } else {
               products.add(product);
            }
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }


   private boolean isProductPage(Document document) {
      return document.select(".productName").first() != null;
   }

   private String crawlDescription(Document document) {
      String description = "";

      Element shortDesc = document.select(".section-product-info").first();

      if (shortDesc != null) {
         description = description + shortDesc.html();
      }

      Element descElement = document.select(".section-specification").first();

      if (descElement != null) {
         description = description + descElement.outerHtml();
      }

      return description;
   }

   private List<Pair<Double, Integer>> fetchQuantities(String internalId) {
      List<Pair<Double, Integer>> quantities = new ArrayList<>();

      JSONObject apiJson = fetchPackAPI(internalId);

      if (apiJson.has("fixedPrices") && apiJson.get("fixedPrices") instanceof JSONArray) {
         for (Object obj : apiJson.getJSONArray("fixedPrices")) {
            if (obj instanceof JSONObject) {
               JSONObject pack = (JSONObject) obj;

               if (pack.has("tradePolicyId") && pack.get("tradePolicyId") instanceof String && pack.getString("tradePolicyId").equals("2")) {
                  if (pack.has("listPrice") && pack.get("listPrice") instanceof Double && pack.has("minQuantity") && pack.get("minQuantity") instanceof Integer) {
                     quantities.add(new Pair<Double, Integer>(pack.getDouble("listPrice"), pack.getInt("minQuantity")));
                  }
               }
            }
         }
      }

      return quantities;
   }

   private Prices buildPackPrices(Float price) {
      Prices prices = new Prices();

      if (price != null) {
         Map<Integer, Float> installmentPriceMap = new HashMap<>();
         installmentPriceMap.put(1, price);

         prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      }

      return prices;
   }

   private JSONObject fetchPackAPI(String skuId) {

      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

      String payload = "sku_id=" + skuId;

      Request request = RequestBuilder.create()
            .setCookies(cookies)
            .setHeaders(headers)
            .setUrl(PACKAGE_API_URL)
            .setPayload(payload)
            .build();

      String response = new FetcherDataFetcher().post(session, request).getBody();
      response = response.replace("\\\"", "\"");

      if (response.startsWith("\"")) {
         response = response.substring(1);
      }

      if (response.endsWith("\"")) {
         response = response.substring(0, response.length() - 1);
      }

      return JSONUtils.stringToJson(response);
   }
}
