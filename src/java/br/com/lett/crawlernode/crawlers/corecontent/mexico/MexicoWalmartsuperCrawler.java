package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import java.util.*;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import org.apache.http.HttpHeaders;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JavanetDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
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
import org.jsoup.Jsoup;

/**
 *
 * 1) Only one sku per page.
 *
 * Price crawling notes: 1) In time crawler was made, there no product unnavailable. 2) There is no
 * bank slip (boleto bancario) payment option. 3) There is no installments for card payment. So we
 * only have 1x payment, and to this value we use the cash price crawled from the sku page. (nao
 * existe divisao no cartao de credito).
 *
 * @author Gabriel Dornelas
 *
 */
public class MexicoWalmartsuperCrawler extends Crawler {

   private static final String HOME_PAGE = "https://super.walmart.com.mx";

   public MexicoWalmartsuperCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.JSOUP);
   }

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
            "https://super.walmart.com.mx/api/rest/model/atg/commerce/catalog/ProductCatalogActor/getSkuSummaryDetails?storeId=0000009999&upc="
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
        // String secondaryImages = crawlSecondaryImages(internalId);
         String description = crawlDescription(apiJson);
         Integer stock = null;
         String ean = internalId;
         List<String> eans = new ArrayList<>();
         eans.add(ean);

         // Creating the product
         Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setName(name).setPrice(price)
               .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
               .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage)
            //.setSecondaryImages(secondaryImages)
            .setDescription(description)
               .setStock(stock).setMarketplace(new Marketplace()).setEans(eans).build();

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

   /**
    * Não achei imagens secundarias
    *
    * @param document
    * @return
    */

   private String crawlSecondaryImages(String id) {
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      for (int i = 1; i < 4; i++) {
         String img = "https://res.cloudinary.com/walmart-labs/image/upload/w_960,dpr_auto,f_auto,q_auto:best/gr/images/product-images/img_large/" + id + "L" + i + ".jpg";
         Request request = RequestBuilder.create().setUrl(img).setCookies(cookies).build();
         Response response = this.dataFetcher.get(session, request);
         RequestsStatistics resp = CommonMethods.getLast(response.getRequests());

         Map<String, String> headers = response.getHeaders();
         if (headers.containsKey(HttpHeaders.CONTENT_TYPE.toLowerCase())) {
            // We get this header because sometimes the image url will return a html
            String content = headers.get(HttpHeaders.CONTENT_TYPE.toLowerCase());

            if (resp != null && resp.getStatusCode() > 0 && resp.getStatusCode() < 400 && (content.contains("image") || content.contains("img"))) {
               secondaryImagesArray.put(img);
            }
         }
      }


      if (secondaryImagesArray.length() > 0) {
         secondaryImages = secondaryImagesArray.toString();
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

   /**
    * There is no bankSlip price.
    *
    * There is no card payment options, other than cash price. So for installments, we will have only
    * one installment for each card brand, and it will be equals to the price crawled on the sku main
    * page.
    *
    * @param doc
    * @param price
    * @return
    */
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
