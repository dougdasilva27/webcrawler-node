package br.com.lett.crawlernode.crawlers.corecontent.brasil;

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

public class BrasilDakiAppCrawler extends Crawler {
   public BrasilDakiAppCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.JSON);
   }

   private final String hubId = getHubId();

   protected String getHubId() {
      return session.getOptions().optString("hub_id");
   }

   @Override
   protected Response fetchResponse() {
      String idProduct = session.getOriginalURL().split("#").length > 1 ? session.getOriginalURL().split("#")[1] : session.getOriginalURL();

      String payload = "{\n" +
         "  \"operationName\": \"singleProduct\",\n" +
         "  \"variables\": {\n" +
         "    \"sku\": \"" + idProduct + "\"\n" +
         "  },\n" +
         "  \"query\": \"query singleProduct($sku: String!) {\\n  products(where: {sku_in: [$sku]}) {\\n    ...AdditionalProductDetailFields\\n    __typename\\n  }\\n}\\n\\nfragment AdditionalProductDetailFields on Product {\\n  sku\\n  vendor_name\\n  alcohol_percent_by_volume\\n  packshot1_front_grid {\\n    url\\n    __typename\\n  }\\n  optimized2_other\\n  optimized1_packshot_prop\\n  innerpack1\\n  packshot3_back\\n  ingredients1_cutout\\n  ingredients2_cutout\\n  ingredients3_cutout\\n  nutritional1_cutout\\n  nutritional2_cutout\\n  nutritional3_cutout\\n  additional1_cutout\\n  additional2_cutout\\n  additional3_cutout\\n  additional4_cutout\\n  packshot4_left\\n  packshot5_right\\n  packshot6_top\\n  packshot7_bottom\\n  long_description\\n  __typename\\n}\\n\"\n" +
         "}";

      Response response = postApiRequest(payload);
      return response;
   }

   protected Response postApiRequest(String payload) {
      HashMap<String, String> headers = new HashMap<>();
      headers.put("accept-encoding", "gzip");
      headers.put("connection", "Keep-Alive");
      headers.put("content-type", "application/json");
      headers.put("host", "api-prd-br.jokrtech.com");
      headers.put("x-brand-name", "daki");

      Request request = Request.RequestBuilder.create()
         .setUrl("https://api-prd-br.jokrtech.com/")
         .setPayload(payload)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_ROTATE_BR,
            ProxyCollection.BUY
         ))
         .build();

      Response response = CrawlerUtils.retryRequest(request, session, new JsoupDataFetcher(), false);
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
         String primaryImage = getPrimaryImage(productSku);

         String name = getNameBySku(internalPid);
         if (name == null) {
            name = getAlternativeName(internalPid);
         }

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

   private String getAlternativeName(String internalPid) {
      String payload = "{\"operationName\":\"ProductDetails\",\"variables\":{\"hubId\":\"" + hubId + "\",\"where\":{\"sku_in\":[\"" + internalPid + "\"]}},\"query\":\"query ProductDetails($where: ProductFilters!, $hubId: String!) {\\n  products(where: $where) {\\n    brand\\n    category {\\n      cmsMainCategory {\\n        title\\n        __typename\\n      }\\n      __typename\\n    }\\n    inventory(hubId: $hubId) {\\n      quantity\\n      showOutOfStock\\n      status\\n      __typename\\n    }\\n    long_description\\n    ui_content_1\\n    packshot1_front_grid {\\n      url\\n      __typename\\n    }\\n    product_status\\n    price(hubId: $hubId) {\\n      amount\\n      compareAtPrice\\n      discount\\n      id\\n      sku\\n      __typename\\n    }\\n    name\\n    title\\n    sku\\n    tags\\n    __typename\\n  }\\n}\"}";
      Response response = postApiRequest(payload);
      JSONObject json = JSONUtils.stringToJson(response.getBody());

      return JSONUtils.getValueRecursive(json, "data.products.0.name", String.class, "");
   }

   private String getPrimaryImage(JSONObject productSku) {
      String primaryImage = JSONUtils.getValueRecursive(productSku, "packshot1_front_grid.url", String.class);

      if (primaryImage != null) {
         if (primaryImage.contains("small_")) {
            return primaryImage.replace("small_", "");
         }
      }

      return primaryImage;
   }

   private String getNameBySku(String internalPid) {
      String payload = "{\n" +
         "  \"operationName\": \"allCategories\",\n" +
         "  \"variables\": {\n" +
         "    \"hubId\": \"" + hubId + "\"\n" +
         "  },\n" +
         "  \"query\": \"query allCategories($hubId: String!) {\\n  availableCategories(hubId: $hubId) {\\n    hubId\\n    categories {\\n      cmsMainCategory {\\n        jokr_id\\n        title\\n        list_image_three {\\n          url(transformation: {image: {resize: {width: 200, height: 200, fit: clip}}})\\n          __typename\\n        }\\n        __typename\\n      }\\n      subCategories {\\n        cmsSubCategory {\\n          jokr_id\\n          title\\n          __typename\\n        }\\n        products {\\n          cmsProduct {\\n            ...CoreProductFields\\n            __typename\\n          }\\n          __typename\\n        }\\n        __typename\\n      }\\n      __typename\\n    }\\n    __typename\\n  }\\n}\\n\\nfragment CoreProductFields on Product {\\n  sku\\n  title\\n  title2\\n  packshot1_front_grid_small: packshot1_front_grid {\\n    url(transformation: {image: {resize: {width: 266, height: 266, fit: clip}}})\\n    __typename\\n  }\\n  ui_content_1\\n  ui_content_1_uom\\n  ui_content_2\\n  ui_content_2_uom\\n  tags\\n  category {\\n    cmsMainCategory {\\n      jokr_id\\n      title\\n      __typename\\n    }\\n    __typename\\n  }\\n  subCategory {\\n    cmsSubCategory {\\n      jokr_id\\n      title\\n      __typename\\n    }\\n    __typename\\n  }\\n  __typename\\n}\\n\"\n" +
         "}";

      Response response = postApiRequest(payload);
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
               JSONObject cmsProduct = JSONUtils.getValueRecursive(product, "cmsProduct", JSONObject.class);
               String cmsProductSku = cmsProduct.optString("sku");

               if (cmsProductSku.equals(internalPid)) {
                  String title = cmsProduct.optString("title");
                  String content1 = cmsProduct.optString("ui_content_1", "");
                  String content1UOM = cmsProduct.optString("ui_content_1_uom", "");
                  String content2 = cmsProduct.optString("ui_content_2", "");
                  String content2UOM = cmsProduct.optString("ui_content_2_uom", "");

                  StringBuilder titleBuilder = new StringBuilder(title);
                  titleBuilder.append(" - ").append(content1).append(content1UOM).append(" ").append(content2).append(content2UOM);

                  return titleBuilder.toString();
               }
            }
         }
      }

      return null;
   }

   protected Map<String, JSONObject> getOffersSku() {
      String payload = "{\n" +
         "  \"operationName\": \"dynamicProductFields\",\n" +
         "  \"variables\": {\n" +
         "    \"hubId\": \"" + hubId + "\"\n" +
         "  },\n" +
         "  \"query\": \"query dynamicProductFields($hubId: String!) {\\n  availableCategories(hubId: $hubId) {\\n    hubId\\n    products {\\n      sku\\n      price(hubId: $hubId) {\\n        amount\\n        compareAtPrice\\n        __typename\\n      }\\n      inventory(hubId: $hubId) {\\n        quantity\\n        maxQuantity\\n        __typename\\n      }\\n      base_price_relevant\\n      pum_conv_factor\\n      standard_pricing_unit\\n      __typename\\n    }\\n    __typename\\n  }\\n}\\n\"\n" +
         "}";

      Response response = postApiRequest(payload);
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
         .setSellerFullName("daki")
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


