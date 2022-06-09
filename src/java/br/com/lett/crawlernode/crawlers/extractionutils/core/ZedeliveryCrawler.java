package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
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
import org.json.JSONObject;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ZedeliveryCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.ze.delivery";
   private static final String API_URL = "https://api.ze.delivery/public-api";
   private String visitorId;

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public ZedeliveryCrawler(Session session) {
      super(session);
      config.setParser(Parser.JSON);
   }

   public String getSellerName() {
      return session.getMarket().getName();
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   private List<String> proxies = Arrays.asList(
      ProxyCollection.BUY,
      ProxyCollection.LUMINATI_SERVER_BR,
      ProxyCollection.NETNUT_RESIDENTIAL_BR,
      ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
      ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
      ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY);

   @Override
   public void handleCookiesBeforeFetch() {
      Request request = Request.RequestBuilder.create()
         .setUrl("https://www.ze.delivery/produtos")
         .setProxyservice(proxies)
         .setSendUserAgent(false)
         .build();
      Response response = new ApacheDataFetcher().get(session, request);


      this.cookies = response.getCookies();
   }

   /**
    * Replicating the first api call of the web, that is used to validate the UUID Making a call
    * sending an address: postal code 05426-100 Address is hard coded in payload
    */
   private JSONObject validateUUID() {

      String initPayload = "{\n" +
         "  \"operationName\": \"setDeliveryOption\",\n" +
         "  \"variables\": {\n" +
         "    \"deliveryOption\": {\n" +
         "      \"address\": {\n" +
         "        \"latitude\": " + session.getOptions().optString("latitude") + ",\n" +
         "        \"longitude\": " + session.getOptions().optString("longitude") + ",\n" +
         "        \"zipcode\": \"" + session.getOptions().optString("zipCode") + "\",\n" +
         "        \"street\": \"" + session.getOptions().optString("street") + "\",\n" +
         "        \"neighborhood\": \"" + session.getOptions().optString("neighborhood") + "\",\n" +
         "        \"city\": \"" + session.getOptions().optString("city") + "\",\n" +
         "        \"province\": \"" + session.getOptions().optString("province") + "\",\n" +
         "        \"country\": \"BR\",\n" +
         "        \"number\": \"" + session.getOptions().optString("number") + "\",\n" +
         "        \"referencePoint\": \"\"\n" +
         "      },\n" +
         "      \"deliveryMethod\": \"DELIVERY\",\n" +
         "      \"schedule\": \"NOW\"\n" +
         "    },\n" +
         "    \"forceOverrideProducts\": false\n" +
         "  },\n" +
         "  \"query\": \"mutation setDeliveryOption($deliveryOption: DeliveryOptionInput, $forceOverrideProducts: Boolean) {\\n  manageCheckout(deliveryOption: $deliveryOption, forceOverrideProducts: $forceOverrideProducts) {\\n    messages {\\n      category\\n      target\\n      key\\n      args\\n      message\\n    }\\n    checkout {\\n      id\\n      deliveryOption {\\n        address {\\n          latitude\\n          longitude\\n          zipcode\\n          country\\n          province\\n          city\\n          neighborhood\\n          street\\n          number\\n          addressLine2\\n          referencePoint\\n        }\\n        deliveryMethod\\n        schedule\\n        scheduleDateTime\\n        pickupPoc {\\n          id\\n          tradingName\\n          address {\\n            latitude\\n            longitude\\n            zipcode\\n            country\\n            province\\n            city\\n            neighborhood\\n            street\\n            number\\n            addressLine2\\n            referencePoint\\n          }\\n        }\\n      }\\n      paymentMethod {\\n        id\\n        displayName\\n      }\\n    }\\n  }\\n}\\n\"\n" +
         "}";

      Request request = Request.RequestBuilder.create().setUrl(API_URL)
         .setPayload(initPayload)
         .setHeaders(getHeaders())
         //coloquei o proxy para afetar todos os ze deliveries 
         .setProxyservice(proxies)
         .setCookies(cookies)
         .setSendUserAgent(true)
         .mustSendContentEncoding(true)
         .build();

      Response response = new JsoupDataFetcher().post(session, request);

      visitorId = response.getHeaders().get("x-visitorid");
      if (!response.isSuccess() || visitorId == null) {
         response = retryRequest(request, List.of(new FetcherDataFetcher(), new ApacheDataFetcher()));
         visitorId = response.getHeaders().get("x-visitorid");
      }

      if (visitorId == null || visitorId.isEmpty()) {
         Logging.printLogError(logger, "FAILED TO GET VISITOR ID");

      }

      return CrawlerUtils.stringToJson(response.getBody());
   }

   private Map<String, String> getHeaders() {
      Map<String, String> headers = new HashMap<>();
      headers.put("Accept-Encoding", "gzip, deflate, br");
      headers.put("Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");
      headers.put("content-type", "application/json");
      headers.put("Origin", "https://www.ze.delivery");
      headers.put("Referer", "https://www.ze.delivery/");
      headers.put("x-request-origin", "WEB");

      return headers;
   }

   @Override
   protected Response fetchResponse() {
      JSONObject apiJson = validateUUID();
      Map<String, String> headers = getHeaders();
      if (!apiJson.isEmpty()) {
         headers.put("x-visitorid", visitorId);
      }

      String payload = "{\"operationName\":\"loadProduct\",\"variables\":{\"id\":\"" + getIdFromUrl() + "\",\"isVisitor\":false},\"query\":\"query loadProduct($id: ID, $isVisitor: Boolean!) {\\n  loadProduct(id: $id, isVisitor: $isVisitor) {\\n    id\\n    displayName\\n    description\\n    isRgb\\n    price {\\n      min\\n      max\\n    }\\n    images\\n    category {\\n      id\\n      displayName\\n    }\\n    brand {\\n      id\\n      displayName\\n    }\\n    applicableDiscount {\\n      discountType\\n      finalValue\\n      presentedDiscountValue\\n    }\\n  }\\n}\\n\"}";
      Request request = Request.RequestBuilder.create()
         .setUrl(API_URL)
         .setHeaders(headers)
         .setPayload(payload)
         .setCookies(cookies)
         .setProxyservice(proxies)
         .setSendUserAgent(false)
         .mustSendContentEncoding(true)
         .build();

      Response response = new JsoupDataFetcher().post(session, request);
      if (!response.isSuccess() || CrawlerUtils.stringToJson(response.getBody()).has("errors")) {
         response = retryRequest(request, List.of(new FetcherDataFetcher(), new JsoupDataFetcher()));
      }

      return response;
   }

   private Response retryRequest(Request request, List<DataFetcher> dataFetcherList) {
      Response response = dataFetcherList.get(0).get(session, request);

      if (!response.isSuccess()) {
         int tries = 0;
         while (!response.isSuccess() && tries < 3) {
            tries++;
            if (tries % 2 == 0) {
               response = dataFetcherList.get(1).get(session, request);
            } else {
               response = dataFetcherList.get(0).get(session, request);
            }
         }
      }

      return response;
   }


   private String getIdFromUrl() {
      String id = null;
      Pattern pattern = Pattern.compile("produto\\/([0-9]+)\\/");
      Matcher matcher = pattern.matcher(session.getOriginalURL());
      if (matcher.find()) {
         id = matcher.group(1);
      }
      return id;
   }

   @Override
   public List<Product> extractInformation(JSONObject jsonObject) throws Exception {
      super.extractInformation(jsonObject);
      List<Product> products = new ArrayList<>();
      JSONObject productJson = JSONUtils.getValueRecursive(jsonObject, "data.loadProduct", JSONObject.class, new JSONObject());

      if (productJson != null && !productJson.isEmpty()) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = productJson.optString("id");
         String name = productJson.optString("displayName");
         String description = productJson.optString("description");
         Offers offers = scrapOffers(productJson);
         String primaryImage = JSONUtils.getValueRecursive(productJson, "images.0", String.class, null);

         Product product = ProductBuilder.create().setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setDescription(description)
            .setOffers(offers)
            .build();
         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page:   " + this.session.getOriginalURL());
      }
      return products;
   }

   private Offers scrapOffers(JSONObject productJson) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(productJson);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));

      offers.add(new Offer.OfferBuilder()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(getSellerName())
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Pricing scrapPricing(JSONObject productJson) throws MalformedPricingException {
      JSONObject dataPrice = JSONUtils.getJSONValue(productJson, "price");
      JSONObject applicableDiscount = JSONUtils.getJSONValue(productJson, "applicableDiscount");
      Double spotlightPrice = null;
      Double priceFrom = null;

      if (!applicableDiscount.isEmpty()) {
         spotlightPrice = JSONUtils.getDoubleValueFromJSON(applicableDiscount, "finalValue", true);
      }
      if (!dataPrice.isEmpty()) {
         priceFrom = JSONUtils.getDoubleValueFromJSON(dataPrice, "min", true);
         if (spotlightPrice == null) {
            spotlightPrice = priceFrom;
            priceFrom = null;
         }
      }

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
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
}
