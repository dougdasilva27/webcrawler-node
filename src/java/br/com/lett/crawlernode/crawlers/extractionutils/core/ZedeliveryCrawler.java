package br.com.lett.crawlernode.crawlers.extractionutils.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import com.google.common.collect.Sets;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;

public abstract class ZedeliveryCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.ze.delivery";
   private static final String API_URL = "https://api.ze.delivery/public-api";
   private static final String SELLER_FULL_NAME = "zedelivery";
   private String visitorId;

   protected abstract ZedeliveryInfo getZedeliveryInfo();

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public ZedeliveryCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   /**
    * Replicating the first api call of the web, that is used to validate the UUID Making a call
    * sending an address: postal code 05426-100 Address is hard coded in payload
    */
   private void validateUUID() {
      visitorId = UUID.randomUUID().toString();

      Map<String, String> headers = new HashMap<>();
      headers.put("x-visitorid", visitorId);
      headers.put("content-type:", "application/json");

      ZedeliveryInfo zeDeliveryInfo = getZedeliveryInfo();

      String initPayload = "{\"operationName\":\"setDeliveryOption\",\"variables\":{\"deliveryOption\":{\"address\":{\"latitude\":" + zeDeliveryInfo.getLatitude()
            + ",\"longitude\":" + zeDeliveryInfo.getLongitude() + ",\"zipcode\":null,\"street\":\"" + zeDeliveryInfo.getStreet() + "\""
            + ",\"neighborhood\":\"" + zeDeliveryInfo.getNeighborhood() + "\",\"city\":\"" + zeDeliveryInfo.getCity() + "\","
            + "\"province\":\"" + zeDeliveryInfo.getProvince() + "\",\"country\":\"BR\",\"number\":\"1\"},\"deliveryMethod\":\"DELIVERY\","
            + "\"schedule\":\"NOW\"},\"forceOverrideProducts\":false},"
            + "\"query\":\"mutation setDeliveryOption($deliveryOption: DeliveryOptionInput, $forceOverrideProducts: Boolean) {\\n  manageCheckout(deliveryOption:"
            + " $deliveryOption, forceOverrideProducts: $forceOverrideProducts) {\\n    messages {\\n      category\\n      target\\n      key\\n      args\\n"
            + "      message\\n    }\\n    checkout {\\n      id\\n      deliveryOption {\\n        address {\\n          latitude\\n          longitude\\n"
            + "          zipcode\\n          country\\n          province\\n          city\\n          neighborhood\\n          street\\n          number\\n"
            + "          addressLine2\\n        }\\n        deliveryMethod\\n        schedule\\n        scheduleDateTime\\n        pickupPoc {\\n          id\\n"
            + "          tradingName\\n          address {\\n            latitude\\n            longitude\\n            zipcode\\n            country\\n"
            + "            province\\n            city\\n            neighborhood\\n            street\\n            number\\n            addressLine2\\n          }\\n"
            + "        }\\n      }\\n      paymentMethod {\\n        id\\n        displayName\\n      }\\n    }\\n  }\\n}\\n\"}";

      Request request = Request.RequestBuilder.create().setUrl(API_URL)
            .setPayload(initPayload)
            .setCookies(cookies)
            .setHeaders(headers)
            .mustSendContentEncoding(false)
            .build();
      this.dataFetcher.post(session, request);
   }

   private JSONObject fetchJson(String id) {
      validateUUID();

      Map<String, String> headers = new HashMap<>();
      headers.put("x-visitorid", visitorId);
      headers.put("content-type:", "application/json");

      String payload = "{\"variables\":{\"isVisitor\":false,\"id\":\"" + id + "\"},\"query\":\"query loadProduct($id: ID, $isVisitor: Boolean!)"
            + "{loadProduct(id: $id, isVisitor: $isVisitor) {id displayName description isRgb price {min max} images category {id displayName} brand "
            + "{id displayName} applicableDiscount {discountType finalValue presentedDiscountValue}}}\",\"operationName\":\"loadProduct\"}";

      Request request = Request.RequestBuilder.create().setUrl(API_URL)
            .setPayload(payload)
            .setCookies(cookies)
            .setHeaders(headers)
            .mustSendContentEncoding(false)
            .build();
      Response response = this.dataFetcher.post(session, request);
      return CrawlerUtils.stringToJson(response.getBody());
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      JSONObject jsonObject = JSONUtils.stringToJson(doc.selectFirst("#__NEXT_DATA__").data());
      JSONObject props = JSONUtils.getJSONValue(jsonObject, "props");
      JSONObject pageProps = JSONUtils.getJSONValue(props, "pageProps");
      String productId = pageProps.optString("productId");

      JSONObject apiJson = fetchJson(productId);

      JSONObject data = apiJson.optJSONObject("data");
      if (data != null) {
         JSONObject loadProduct = data.optJSONObject("loadProduct");
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = loadProduct.optString("id");
         String internalPId = internalId;
         String name = loadProduct.optString("displayName");
         String description = loadProduct.optString("description");
         JSONArray imageArray = loadProduct.optJSONArray("images");
         String primaryImage = imageArray.getString(0);
         String secondaryImage = scrapSecondaryImages(imageArray, primaryImage);

         JSONObject categories = loadProduct.optJSONObject("category");
         String category = categories.optString("displayName");
         Offers offers = scrapOffer(loadProduct);

         Product product = ProductBuilder.create().setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPId)
               .setName(name)
               .setCategory1(category)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImage)
               .setDescription(description)
               .setOffers(offers)
               .build();
         products.add(product);
      }
      return products;
   }

   private Offers scrapOffer(JSONObject product) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(product);

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

   private Pricing scrapPricing(JSONObject product) throws MalformedPricingException {
      JSONObject discountPrice = product.optJSONObject("applicableDiscount");
      JSONObject prices = product.optJSONObject("price");

      Double spotlightPrice = discountPrice != null ? discountPrice.optDouble("finalValue") : prices.optDouble("min");
      spotlightPrice = Math.round(spotlightPrice * 100) / 100.0;

      Double priceFrom = prices.optDouble("min") != spotlightPrice ? prices.optDouble("min") : null;

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
            .setSpotlightPrice(spotlightPrice)
            .setPriceFrom(priceFrom)
            .setCreditCards(creditCards)
            .setBankSlip(BankSlipBuilder.create().setFinalPrice(spotlightPrice).build())
            .build();
   }


   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      installments.add(InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());

      for (String brand : cards) {
         creditCards.add(CreditCardBuilder.create()
               .setBrand(brand)
               .setIsShopCard(false)
               .setInstallments(installments)
               .build());
      }
      return creditCards;
   }

   private String scrapSecondaryImages(JSONArray imageArray, String primaryImage) {
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      for (int i = 1; i < imageArray.length(); i++) {
         String image = imageArray.getString(i);
         if (!image.equals(primaryImage)) {
            secondaryImagesArray.put(image);
         }
      }

      if (secondaryImagesArray.length() > 0) {
         secondaryImages = secondaryImagesArray.toString();
      }
      return secondaryImages;
   }

   public static class ZedeliveryInfo {
      private String longitude;
      private String latitude;
      private String street;
      private String neighborhood;
      private String city;
      private String province;

      public String getLongitude() {
         return longitude;
      }

      public String getLatitude() {
         return latitude;
      }

      public String getStreet() {
         return street;
      }

      public String getNeighborhood() {
         return neighborhood;
      }

      public String getCity() {
         return city;
      }

      public String getProvince() {
         return province;
      }

      public void setLongitude(String longitude) {
         this.longitude = longitude;
      }

      public void setLatitude(String latitude) {
         this.latitude = latitude;
      }

      public void setStreet(String street) {
         this.street = street;
      }

      public void setNeighborhood(String neighborhood) {
         this.neighborhood = neighborhood;
      }

      public void setCity(String city) {
         this.city = city;
      }

      public void setProvince(String province) {
         this.province = province;
      }
   }

   public static class ZedeliveryInfoBuilder {
      private String longitude = null;
      private String latitude = null;
      private String street = null;
      private String neighborhood = null;
      private String city = null;
      private String province = null;

      public static ZedeliveryInfoBuilder create() {
         return new ZedeliveryInfoBuilder();
      }

      public ZedeliveryInfoBuilder setLongitude(String longitude) {
         this.longitude = longitude;
         return this;
      }

      public ZedeliveryInfoBuilder setLatitude(String latitude) {
         this.latitude = latitude;
         return this;
      }

      public ZedeliveryInfoBuilder setStreet(String street) {
         this.street = street;
         return this;
      }

      public ZedeliveryInfoBuilder setNeighborhood(String neighborhood) {
         this.neighborhood = neighborhood;
         return this;
      }

      public ZedeliveryInfoBuilder setCity(String city) {
         this.city = city;
         return this;
      }

      public ZedeliveryInfoBuilder setProvince(String province) {
         this.province = province;
         return this;
      }

      public ZedeliveryInfo build() {
         ZedeliveryInfo ze = new ZedeliveryInfo();

         ze.setCity(this.city);
         ze.setLatitude(this.latitude);
         ze.setLongitude(this.longitude);
         ze.setNeighborhood(this.neighborhood);
         ze.setProvince(this.province);
         ze.setStreet(this.street);

         return ze;
      }
   }
}
