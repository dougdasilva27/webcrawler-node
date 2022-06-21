package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.RequestsStatistics;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Marketplace;
import models.Offer;
import models.Offers;
import models.prices.Prices;
import models.pricing.CreditCards;
import models.pricing.Pricing;
import org.apache.http.HttpHeaders;
import org.eclipse.jetty.util.ajax.JSON;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class WalmartSuperCrawler extends Crawler {

   private static final String HOME_PAGE = "https://super.walmart.com.mx";
   private static final String SELLER_FULL_NAME = "Walmart Super Mexico";
   private Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString());

   public WalmartSuperCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.JSOUP);
      super.config.setParser(Parser.JSON);
   }

   String store_id  = session.getOptions().optString("store_id");

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   protected Response fetchResponse() {
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
               ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_DE_HAPROXY))
         .setFetcheroptions(FetcherOptions.FetcherOptionsBuilder.create().setForbiddenCssSelector(".Mantn-presionado-el").build())
         .build();


      Request requestFetcher = Request.RequestBuilder.create()
         .setUrl(apiUrl)
         .setProxyservice(
            Arrays.asList(
               ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_DE_HAPROXY))
         .build();

      Request request = dataFetcher instanceof FetcherDataFetcher ? requestFetcher : requestJsoup;

      Response response = CrawlerUtils.retryRequest(request, session, dataFetcher);

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

      return response;
   }

   @Override
   public List<Product> extractInformation(JSONObject apiJson) throws Exception {
      List<Product> products = new ArrayList<>();

      if (apiJson.has("skuId")) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = apiJson.optString("skuId");
         String name = apiJson.optString("skuDisplayNameText");
         String primaryImage = crawlPrimaryImage(internalId);
         List<String> secondaryImages = crawlSecondaryImages(internalId);
         CategoryCollection categories = crawlCategories(apiJson);
         String description = crawlDescription(apiJson);
         List<String> eans = new ArrayList<>();
         eans.add(internalId);
         boolean available = crawlAvailability(apiJson);
         Offers offers = available ? crawlOffers(apiJson, internalId) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setEans(eans)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private Offers crawlOffers(JSONObject apiJson, String internalId) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(apiJson, internalId);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .build());

      return offers;
   }

   private Pricing scrapPricing(JSONObject apiJson, String internalId) throws MalformedPricingException {
      String spotlightPriceString = apiJson.optString("specialPrice");
      String priceFromString = apiJson.optString("basePrice");
      Double spotlightPrice = null;
      Double priceFrom = null;

      if(spotlightPriceString.isEmpty()){
         JSONObject priceObject = crawlPriceApi(internalId);
         spotlightPrice = CommonMethods.objectToDouble(priceObject.optQuery("/skuinfo/" + internalId + "_" + store_id + "/activeSpecialPrice"));
         priceFrom = CommonMethods.objectToDouble(priceObject.optQuery("/skuinfo/" + internalId + "_" + store_id + "/activeOriginalPrice"));
      } else {
         spotlightPrice = Double.valueOf(spotlightPriceString);
         priceFrom = Double.valueOf(priceFromString);
      }

      if(Objects.equals(spotlightPrice, priceFrom)){
         priceFrom = null;
      }

      CreditCards creditCards = CrawlerUtils.scrapCreditCards(spotlightPrice, cards);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .build();
   }

   private JSONObject crawlPriceApi(String internalId) {
      Request request = Request.RequestBuilder.create()
         .setUrl("https://super.walmart.com.mx/api/rest/model/atg/commerce/catalog/ProductCatalogActor/getSkuPriceInventoryPromotions?skuId="+internalId+"&storeId=" + store_id)
         .setProxyservice(
            Arrays.asList(
               ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_DE_HAPROXY))
         .build();

      return  CrawlerUtils.stringToJson(dataFetcher.get(session, request).getBody());
   }

   private boolean crawlAvailability(JSONObject apiJson) {
      boolean available = false;

      if (apiJson.has("status")) {
         String status = apiJson.optString("status");

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
         JSONObject breadcrumb = apiJson.optJSONObject("breadcrumb");

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
         JSONObject attributesMap = apiJson.optJSONObject("attributesMap");

         for (String key : attributesMap.keySet()) {
            JSONObject attribute = attributesMap.optJSONObject(key);

            if (attribute != null && attribute.optJSONObject("attrGroupId") != null) {
               JSONObject attrGroupId = attribute.optJSONObject("attrGroupId");

               if (attrGroupId.optString("optionValue") != null) {
                  String optionValue = attrGroupId.optString("optionValue");

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
}

