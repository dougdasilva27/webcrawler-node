package br.com.lett.crawlernode.crawlers.extractionutils.core;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import models.pricing.*;
import org.apache.commons.lang3.StringUtils;
import org.jooq.util.derby.sys.Sys;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
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
import models.pricing.Installment.InstallmentBuilder;
import org.jsoup.nodes.Element;

public abstract class ZedeliveryCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.ze.delivery";
   private static final String API_URL = "https://api.ze.delivery/public-api";
   private static final String SELLER_NAME = "zedelivery";
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
   private JSONObject validateUUID() {
      visitorId = UUID.randomUUID().toString();

      Map<String, String> headers = new HashMap<>();
      headers.put("x-visitorid", visitorId);
      headers.put("content-type:", "application/json");
      ZedeliveryInfo zeDeliveryInfo = getZedeliveryInfo();

      String initPayload = "{\"operationName\":\"setDeliveryOption\",\"variables\":{\"deliveryOption\":" +
         "{\"address\":{\"latitude\":"+ zeDeliveryInfo.getLatitude() +",\"longitude\":"+ zeDeliveryInfo.getLongitude() +"," +
         "\"zipcode\":\""+ zeDeliveryInfo.getZipcode() +"\",\"street\":\""+ zeDeliveryInfo.getStreet() +"\"," +
         "\"neighborhood\":\""+ zeDeliveryInfo.getNeighborhood() +"\",\"city\":\""+ zeDeliveryInfo.getCity() +"\"," +
         "\"province\":\""+ zeDeliveryInfo.getProvince() +"\",\"country\":\"BR\",\"number\":\"45\",\"referencePoint\":\"\"}," +
         "\"deliveryMethod\":\"DELIVERY\",\"schedule\":\"NOW\"},\"forceOverrideProducts\":false}," +
         "\"query\":\"mutation setDeliveryOption($deliveryOption: DeliveryOptionInput, $forceOverrideProducts: Boolean) " +
         "{\\n  manageCheckout(deliveryOption: $deliveryOption, forceOverrideProducts: $forceOverrideProducts) {\\n    " +
         "messages {\\n      category\\n      target\\n      key\\n      args\\n      message\\n    }\\n    checkout {\\n " +
         "     id\\n      deliveryOption {\\n        address {\\n          latitude\\n          longitude\\n          zipcode\\n" +
         "          country\\n          province\\n          city\\n          neighborhood\\n          street\\n          number\\n " +
         "         addressLine2\\n          referencePoint\\n        }\\n        deliveryMethod\\n        schedule\\n        scheduleDateTime\\n " +
         "       pickupPoc {\\n          id\\n          tradingName\\n          address {\\n            latitude\\n            longitude\\n " +
         "           zipcode\\n            country\\n            province\\n            city\\n            neighborhood\\n            street\\n " +
         "           number\\n            addressLine2\\n            referencePoint\\n          }\\n        }\\n      }\\n      paymentMethod {\\n " +
         "       id\\n        displayName\\n      }\\n    }\\n  }\\n}\\n\"}";

      Request request = Request.RequestBuilder.create().setUrl(API_URL)
         .setPayload(initPayload)
         .setCookies(cookies)
         .setHeaders(headers)
//         .setProxyservice(Collections.singletonList(ProxyCollection.NO_PROXY))
         .mustSendContentEncoding(false)
         .build();

      Response response = this.dataFetcher.post(session, request);
      return CrawlerUtils.stringToJson(response.getBody());
   }

   @Override
   protected Document fetch() {
      Map<String, String> headers = new HashMap<>();
      JSONObject apiJson = validateUUID();
      JSONObject userAddress = JSONUtils.getValueRecursive(apiJson,"data.manageCheckout.checkout.deliveryOption.address", JSONObject.class);
      JSONObject deliveryOptions = JSONUtils.getValueRecursive(apiJson,"data.manageCheckout.checkout.deliveryOption", JSONObject.class);
      String cookie = "visitorId=%22" + visitorId +
         "%22; userAddress=" + URLEncoder.encode(userAddress.toString(), StandardCharsets.UTF_8) +
         "; deliveryOptions=" + URLEncoder.encode(deliveryOptions.toString(), StandardCharsets.UTF_8) +";";

      headers.put("Accept","*/*");
      headers.put("Accept-Encoding","gzip, deflate, br");
      headers.put("Connection","keep-alive");
      headers.put("cookie", cookie);

      Request request = Request.RequestBuilder.create()
         .setUrl(session.getOriginalURL())
         .setHeaders(headers)
         .setSendUserAgent(false)
         .build();

      Response a = this.dataFetcher.get(session, request);

      String content = a.getBody();

      return Jsoup.parse(content);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (doc.selectFirst("#add-product") != null) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         String scriptId = CrawlerUtils.scrapScriptFromHtml(doc, "#__NEXT_DATA__");
         JSONObject jsonId = new JSONObject(scriptId.substring(1, scriptId.length()-1));
         String internalId = JSONUtils.getValueRecursive(jsonId,"query.id", String.class);
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.css-aibq80-productTitle", false);
         String description = CrawlerUtils.scrapStringSimpleInfo(doc, "[name=description]", false);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "ul.css-11x3awa-Breadcrumb li a");
         boolean available = doc.selectFirst("#add-product") != null;
         Offers offers = available ? scrapOffers(doc) : new Offers();

         Product product = ProductBuilder.create().setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setPrimaryImage(null)
            .setDescription(description)
            .setOffers(offers)
            .build();
         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page:   " + this.session.getOriginalURL());
      }
      return products;
   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));

      offers.add(new Offer.OfferBuilder()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(this.SELLER_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".css-1jqrnd2-priceText", null, true, ',', session);

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditCards(Double price) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(price)
         .setFinalPrice(price)
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

   public static class ZedeliveryInfo {
      private String longitude;
      private String latitude;
      private String street;
      private String neighborhood;
      private String city;
      private String province;

      public void setReferencePoint(String referencePoint) {
         this.referencePoint = referencePoint;
      }

      public void setNumber(String number) {
         this.number = number;
      }

      public void setZipcode(String zipcode) {
         this.zipcode = zipcode;
      }

      private String referencePoint = "";
      private String number = "100";
      private String zipcode;

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

      public String getZipcode() {
         return zipcode;
      }

      public String getReferencePoint() {
         return referencePoint;
      }

      public String getNumber() {
         return number;
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
      private String referencePoint = "";
      private String number = "100";
      private String zipcode = null;

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

      public ZedeliveryInfoBuilder setReferencePoint(String referencePoint) {
         this.referencePoint = referencePoint;
         return this;
      }

      public ZedeliveryInfoBuilder setNumber(String number) {
         this.number = number;
         return this;
      }

      public ZedeliveryInfoBuilder setZipCode(String zipcode) {
         this.zipcode = zipcode;
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
