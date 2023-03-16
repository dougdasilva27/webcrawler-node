package br.com.lett.crawlernode.crawlers.corecontent.peru;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Parser;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class PeruJokrCrawler extends Crawler {
   public PeruJokrCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.JSON);
      super.config.setFetcher(FetchMode.JSOUP);
   }

   String SELLER_NAME = null;
   private final String hubId = getHubId();

   protected String getHubId() {
      return session.getOptions().optString("hub_id");
   }

   @Override
   protected Response fetchResponse() {
      String payload = "{\n" +
         "  \"operationName\": \"singleProduct\",\n" +
         "  \"variables\": {\n" +
         "    \"sku\": \"" + session.getOriginalURL() + "\"\n" +
         "  },\n" +
         "  \"query\": \"query singleProduct($sku: String!) {\\n  products(where: {sku_in: [$sku]}) {\\n    ...AdditionalProductDetailFields\\n    __typename\\n  }\\n}\\n\\nfragment AdditionalProductDetailFields on Product {\\n  sku\\n  vendor_name\\n  alcohol_percent_by_volume\\n  packshot1_front_grid {\\n    url\\n    __typename\\n  }\\n  optimized2_other\\n  optimized1_packshot_prop\\n  innerpack1\\n  packshot3_back\\n  ingredients1_cutout\\n  ingredients2_cutout\\n  ingredients3_cutout\\n  nutritional1_cutout\\n  nutritional2_cutout\\n  nutritional3_cutout\\n  additional1_cutout\\n  additional2_cutout\\n  additional3_cutout\\n  additional4_cutout\\n  packshot4_left\\n  packshot5_right\\n  packshot6_top\\n  packshot7_bottom\\n  long_description\\n  __typename\\n}\\n\"\n" +
         "}";
      HashMap<String, String> headers = new HashMap<>();
      headers.put("Content-type", "application/json");

      Request request = Request.RequestBuilder.create()
         .setUrl("https://api-prd-pe.jokrtech.com/")
         .setPayload(payload)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY
         ))
         .build();

      Response response = CrawlerUtils.retryRequest(request, session, new JsoupDataFetcher());
      return response;
   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      super.extractInformation(json);
      List<Product> products = new ArrayList<>();

      if (json.has("data")) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         JSONObject productSku = JSONUtils.getValueRecursive(json, "data.products.0", JSONObject.class);
         Map<String, JSONObject> productSkuPrice = new HashMap<>();
         productSkuPrice = getOffersSku();

         String internalPid = productSku.optString("sku");
         String description = productSku.optString("long_description");
         String primaryImage = JSONUtils.getValueRecursive(productSku, "packshot1_front_grid.url", String.class);
         SELLER_NAME = productSku.optString("vendor_name");

         String name = getNamebySku(internalPid);

         boolean isAvailable = productSkuPrice.get(internalPid) != null;
         Offers offers = isAvailable ? scrapOffers(productSkuPrice.get(internalPid)) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalPid)
            .setInternalPid(internalPid)
            .setName(name)
            .setOffers(offers)
            .setPrimaryImage(primaryImage)
            .setDescription(description)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private String getNamebySku(String internalPid) {
      String payload = "{\n" +
         "    \"operationName\": \"allCategories\",\n" +
         "    \"variables\": {\n" +
         "        \"hubId\": \"" + hubId + "\"\n" +
         "    },\n" +
         "    \"query\": \"query allCategories($hubId: String!) {\\n  availableCategories(hubId: $hubId) {\\n    hubId\\n    categories {\\n      cmsMainCategory {\\n        jokr_id\\n        title\\n        list_image_three {\\n          url(transformation: {image: {resize: {width: 200, height: 200, fit: clip}}})\\n          __typename\\n        }\\n        __typename\\n      }\\n      subCategories {\\n        cmsSubCategory {\\n          jokr_id\\n          title\\n          __typename\\n        }\\n        products {\\n          cmsProduct {\\n            ...CoreProductFields\\n            __typename\\n          }\\n          __typename\\n        }\\n        __typename\\n      }\\n      __typename\\n    }\\n    __typename\\n  }\\n}\\n\\nfragment CoreProductFields on Product {\\n  sku\\n  title\\n  title2\\n  packshot1_front_grid_small: packshot1_front_grid {\\n    url(transformation: {image: {resize: {width: 266, height: 266, fit: clip}}})\\n    __typename\\n  }\\n  ui_content_1\\n  ui_content_1_uom\\n  ui_content_2\\n  ui_content_2_uom\\n  tags\\n  __typename\\n}\\n\"\n" +
         "}";

      HashMap<String, String> headers = new HashMap<>();
      headers.put("Content-type", "application/json");

      Request request = Request.RequestBuilder.create()
         .setUrl("https://api-prd-pe.jokrtech.com/")
         .setPayload(payload)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY
         ))
         .build();

      Response response = CrawlerUtils.retryRequest(request, session, new JsoupDataFetcher());
      JSONObject json = JSONUtils.stringToJson(response.getBody());
      JSONArray categories = JSONUtils.getValueRecursive(json, "data.availableCategories.categories", JSONArray.class);
      for (Object categoryObj : categories) {
         JSONObject category = (JSONObject) categoryObj;
         JSONArray subCategories = JSONUtils.getValueRecursive(category, "subCategories", JSONArray.class);
         for (Object subCategoryObj : subCategories) {
            JSONObject subCategory = (JSONObject) subCategoryObj;
            JSONArray products = JSONUtils.getValueRecursive(subCategory, "products", JSONArray.class);
            for (Object productObj : products) {
               JSONObject product = (JSONObject) productObj;
               String cmsProductSku = JSONUtils.getValueRecursive(product, "cmsProduct.sku", String.class);
               if (cmsProductSku.equals(internalPid)) {
                  return JSONUtils.getValueRecursive(product, "cmsProduct.title", String.class);
               }
            }
         }
      }
      return null;
   }

   protected Map<String, JSONObject> getOffersSku() {
      String payload = "{\n" +
         "    \"operationName\": \"dynamicProductFields\",\n" +
         "    \"variables\": {\n" +
         "        \"hubId\": \"" + hubId + "\"\n" +
         "    },\n" +
         "    \"query\": \"query dynamicProductFields($hubId: String!) {\\n  availableCategories(hubId: $hubId) {\\n    hubId\\n    products {\\n      sku\\n      price(hubId: $hubId) {\\n        amount\\n        compareAtPrice\\n        __typename\\n      }\\n      inventory(hubId: $hubId) {\\n        quantity\\n        maxQuantity\\n        __typename\\n      }\\n      base_price_relevant\\n      pum_conv_factor\\n      standard_pricing_unit\\n      __typename\\n    }\\n    __typename\\n  }\\n}\\n\"\n" +
         "}";

      HashMap<String, String> headers = new HashMap<>();
      headers.put("Content-type", "application/json");

      Request request = Request.RequestBuilder.create()
         .setUrl("https://api-prd-pe.jokrtech.com/")
         .setPayload(payload)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY
         ))
         .build();
      Response response = CrawlerUtils.retryRequest(request, session, new JsoupDataFetcher());
      JSONObject jsonObject = JSONUtils.stringToJson(response.getBody());
      JSONArray array = JSONUtils.getValueRecursive(jsonObject, "data.availableCategories.products", JSONArray.class);
      Map<String, JSONObject> productOffers = new HashMap<>();
      for (Object obj : array) {
         JSONObject jsonSku = (JSONObject) obj;
         String sku = jsonSku.optString("sku");
         Integer quantity = JSONUtils.getValueRecursive(jsonSku, "inventory.quantity", Integer.class);

         JSONObject price = quantity > 0 ? jsonSku.optJSONObject("price") : null;

         productOffers.put(sku, price);
      }

      return productOffers;
   }

   protected Offers scrapOffers(JSONObject json) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(json);
      List<String> sales = new ArrayList<>();
      sales.add(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setPricing(pricing)
         .setSales(sales)
         .setSellerFullName(SELLER_NAME)
         .setIsMainRetailer(true)
         .setUseSlugNameAsInternalSellerId(true)
         .build());

      return offers;
   }

   private Pricing scrapPricing(JSONObject json) throws MalformedPricingException {
      Double spotlightPrice = json.optDouble("amount");
      Double priceFrom = json.optDouble("compareAtPrice", 0);
      if (priceFrom == 0) {
         priceFrom = null;
      }

      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditCards(Double spotlighPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();
      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlighPrice)
         .build());

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }
}
