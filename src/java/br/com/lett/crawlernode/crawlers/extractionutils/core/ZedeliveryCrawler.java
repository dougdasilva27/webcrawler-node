package br.com.lett.crawlernode.crawlers.extractionutils.core;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.models.*;
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

public class ZedeliveryCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.ze.delivery";
   private static final String API_URL = "https://api.ze.delivery/public-api";
   private String visitorId;

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public ZedeliveryCrawler(Session session) {
      super(session);
      config.setParser(Parser.HTML);
   }

   public String getSellerName(){
      return session.getMarket().getName();
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
      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/json");

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
         .setHeaders(headers)
         //coloquei o proxy para afetar todos os ze deliveries 
         .setProxyservice(Arrays.asList(ProxyCollection.BUY_HAPROXY,ProxyCollection.LUMINATI_SERVER_BR_HAPROXY,ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY))
         .mustSendContentEncoding(false)
         .build();

      Response response = new JsoupDataFetcher().post(session, request);
      visitorId = response.getHeaders().get("x-visitorid");
      if(visitorId == null || visitorId.isEmpty()) {
         Logging.printLogError(logger, "FAILED TO GET VISITOR ID");
      }
      return CrawlerUtils.stringToJson(response.getBody());
   }

   @Override
   protected Response fetchResponse() {
      Map<String, String> headers = new HashMap<>();
      JSONObject apiJson = validateUUID();
      if(!apiJson.isEmpty()) {
         JSONObject userAddress = JSONUtils.getValueRecursive(apiJson, "data.manageCheckout.checkout.deliveryOption.address", JSONObject.class);
         JSONObject deliveryOptions = JSONUtils.getValueRecursive(apiJson, "data.manageCheckout.checkout.deliveryOption", JSONObject.class);
         String cookie = "visitorId=%22" + visitorId +
            "%22; userAddress=" + URLEncoder.encode(userAddress.toString(), StandardCharsets.UTF_8) +
            "; deliveryOptions=" + URLEncoder.encode(deliveryOptions.toString(), StandardCharsets.UTF_8) + ";";

         headers.put("Accept", "*/*");
         headers.put("Accept-Encoding", "gzip, deflate, br");
         headers.put("Connection", "keep-alive");
         headers.put("cookie", cookie);
      }
      Request request = Request.RequestBuilder.create()
         .setUrl(session.getOriginalURL())
         .setHeaders(headers)
         .setSendUserAgent(false)
         .build();

      return this.dataFetcher.get(session, request);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (doc.selectFirst("#add-product") != null) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         String scriptId = CrawlerUtils.scrapScriptFromHtml(doc, "#__NEXT_DATA__");
         JSONObject jsonId = new JSONObject(scriptId.substring(1, scriptId.length() - 1));
         String internalId = JSONUtils.getValueRecursive(jsonId, "query.id", String.class);
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.css-aibq80-productTitle", false);
         String description = CrawlerUtils.scrapStringSimpleInfo(doc, "[name=description]", false);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "ul.css-11x3awa-Breadcrumb li a");
         boolean available = doc.selectFirst("#add-product") != null;
         Offers offers = available ? scrapOffers(doc) : new Offers();
         String primaryImage = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "[property=\"og:image\"]", "content");

         Product product = ProductBuilder.create().setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
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

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
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
}
