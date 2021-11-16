package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.RequestsStatistics;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;
import org.apache.http.HttpHeaders;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class WalmartSuperCrawler extends Crawler {

   private static final String HOME_PAGE = "https://super.walmart.com.mx";

   public WalmartSuperCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.JSOUP);
   }

   String store_id  = session.getOptions().optString("store_id");

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   protected Object fetch() {
      String url = session.getOriginalURL();

      if (url.contains("?")) {
         url = url.split("\\?")[0];
      }

      String finalParameter = CommonMethods.getLast(url.split("/"));

      if (finalParameter.contains("_")) {
         finalParameter = CommonMethods.getLast(finalParameter.split("_")).trim();
      }

      String apiUrl =
         "https://super.walmart.com.mx/api/rest/model/atg/commerce/catalog/ProductCatalogActor/getSkuSummaryDetails?storeId="+store_id+"&upc="
            + finalParameter + "&skuId=" + finalParameter;

      Request requestJsoup = Request.RequestBuilder.create()
         .setUrl(apiUrl)
         .setCookies(cookies)
         .setProxyservice(
            Arrays.asList(
               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_DE_HAPROXY))
         .build();


      Request requestFetcher = Request.RequestBuilder.create()
         .setUrl(apiUrl)
         .setProxyservice(
            Arrays.asList(
               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_DE_HAPROXY))
         .build();

      Request request = dataFetcher instanceof FetcherDataFetcher ? requestFetcher : requestJsoup;

      Response response = dataFetcher.get(session, request);

      int statusCode = response.getLastStatusCode();

      if ((Integer.toString(statusCode).charAt(0) != '2' &&
         Integer.toString(statusCode).charAt(0) != '3'
         && statusCode != 404)) {

         if (dataFetcher instanceof FetcherDataFetcher) {
            response = new JsoupDataFetcher().get(session, requestJsoup);
         } else {
            response = new FetcherDataFetcher().get(session, requestFetcher);
         }
      }

      return CrawlerUtils.stringToJson(response.getBody());
   }

   @Override
   public List<Product> extractInformation(JSONObject apiJson) throws Exception {
      super.extractInformation(apiJson);
      List<Product> products = new ArrayList<>();

      if (apiJson.has("skuId")) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = crawlInternalId(apiJson);
         String name = crawlName(apiJson);
         Float price = crawlPrice(apiJson);
         Prices prices = crawlPrices(price);
         boolean available = crawlAvailability(apiJson);
         CategoryCollection categories = crawlCategories(apiJson);
         String primaryImage = crawlPrimaryImage(internalId);
         List<String> secondaryImages = crawlSecondaryImages(internalId);
         String description = crawlDescription(apiJson);
         Integer stock = null;
         String ean = internalId;
         List<String> eans = new ArrayList<>();
         eans.add(ean);

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
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
            .setMarketplace(new Marketplace())
            .setEans(eans)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private String crawlInternalId(JSONObject apiJson) {
      String internalId = null;

      if (apiJson.has("skuId")) {
         internalId = apiJson.getString("skuId");
      }

      return internalId;
   }

   private String crawlName(JSONObject apiJson) {
      String name = null;

      if (apiJson.has("skuDisplayNameText")) {
         name = apiJson.getString("skuDisplayNameText");
      }

      return name;
   }

   private Float crawlPrice(JSONObject apiJson) {
      Float price = null;

      if (apiJson.has("specialPrice")) {
         String priceText = apiJson.get("specialPrice").toString().replaceAll("[^0-9.]", "");

         if (!priceText.isEmpty()) {
            price = Float.parseFloat(priceText);
         }
      }

      return price;
   }

   private boolean crawlAvailability(JSONObject apiJson) {
      boolean available = false;

      if (apiJson.has("status")) {
         String status = apiJson.getString("status");

         available = status.equalsIgnoreCase("SELLABLE");
      }

      return available;
   }

   private String crawlPrimaryImage(String id) {

      return "https://res.cloudinary.com/walmart-labs/image/upload/w_960,dpr_auto,f_auto,q_auto:best/gr/images/product-images/img_large/" + id + "L.jpg";
   }

   private List<String> crawlSecondaryImages(String id) {
      List<String> secondaryImages = new ArrayList<>();

      for (int i = 1; i < 4; i++) {
         String img = "https://res.cloudinary.com/walmart-labs/image/upload/w_960,dpr_auto,f_auto,q_auto:best/gr/images/product-images/img_large/" + id + "L" + i + ".jpg";
         secondaryImages.add(img);
      }

      return secondaryImages;
   }

   private CategoryCollection crawlCategories(JSONObject apiJson) {
      CategoryCollection categories = new CategoryCollection();

      if (apiJson.has("breadcrumb") && apiJson.get("breadcrumb") instanceof JSONObject) {
         JSONObject breadcrumb = apiJson.getJSONObject("breadcrumb");

         if (breadcrumb.has("departmentName")) {
            categories.add(breadcrumb.get("departmentName").toString());
         }

         if (breadcrumb.has("familyName")) {
            categories.add(breadcrumb.get("familyName").toString());
         }

         if (breadcrumb.has("fineLineName")) {
            categories.add(breadcrumb.get("fineLineName").toString());
         }
      }

      return categories;
   }

   private String crawlDescription(JSONObject apiJson) {
      StringBuilder description = new StringBuilder();

      if (apiJson.has("longDescription")) {
         description.append("<div id=\"desc\"> <h3> Descripción </h3>");
         description.append(apiJson.get("longDescription") + "</div>");
      }

      StringBuilder nutritionalTable = new StringBuilder();
      StringBuilder caracteristicas = new StringBuilder();

      if (apiJson.has("attributesMap")) {
         JSONObject attributesMap = apiJson.getJSONObject("attributesMap");

         for (String key : attributesMap.keySet()) {
            JSONObject attribute = attributesMap.getJSONObject(key);

            if (attribute.has("attrGroupId")) {
               JSONObject attrGroupId = attribute.getJSONObject("attrGroupId");

               if (attrGroupId.has("optionValue")) {
                  String optionValue = attrGroupId.getString("optionValue");

                  if (optionValue.equalsIgnoreCase("Tabla nutrimental")) {
                     setAPIDescription(attribute, nutritionalTable);
                  } else if (optionValue.equalsIgnoreCase("Características")) {
                     setAPIDescription(attribute, caracteristicas);
                  }
               }
            }
         }
      }

      if (!nutritionalTable.toString().isEmpty()) {
         description.append("<div id=\"table\"> <h3> Nutrición </h3>");
         description.append(nutritionalTable + "</div>");
      }

      if (!caracteristicas.toString().isEmpty()) {
         description.append("<div id=\"short\"> <h3> Características </h3>");
         description.append(caracteristicas + "</div>");
      }

      return description.toString();
   }

   private void setAPIDescription(JSONObject attributesMap, StringBuilder desc) {
      if (attributesMap.has("attrDesc") && attributesMap.has("value")) {
         desc.append("<div>");
         desc.append("<span float=\"left\">" + attributesMap.get("attrDesc") + "&nbsp </span>");
         desc.append("<span float=\"right\">" + attributesMap.get("value") + " </span>");
         desc.append("</div>");
      }
   }

   private Prices crawlPrices(Float price) {
      Prices prices = new Prices();
      prices.setBankTicketPrice(price);
      if (price != null) {
         Map<Integer, Float> installmentPriceMap = new TreeMap<>();
         installmentPriceMap.put(1, price);

         prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      }

      return prices;
   }
}

