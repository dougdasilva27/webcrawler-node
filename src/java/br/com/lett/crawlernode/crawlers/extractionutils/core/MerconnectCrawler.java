package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONObject;

import java.util.*;

public abstract class MerconnectCrawler extends Crawler {

   protected MerconnectCrawler(Session session) {
      super(session);
      config.setFetcher(FetchMode.JSOUP);
   }

   //Client ID and Client Secret can be found in token request. If you cannot found this request in browser, open the website in anonymous mode tracking the requests.
   protected abstract String getClientId();

   protected abstract String getClientSecret();

   //The store id can be found in the product json in the key "marketId"
   protected abstract String getStoreId();

   protected abstract String getSellerName();

   @Override
   protected JSONObject fetch() {
      JSONObject json = new JSONObject();

      String[] splittedUrl = session.getOriginalURL().split("/");

      if (splittedUrl.length > 0) {
         String lastIndex = splittedUrl[splittedUrl.length - 1];
         String url = "https://www.merconnect.com.br/mapp/v2/markets/" + getStoreId() + "/items/" + lastIndex.substring(0, lastIndex.indexOf("?"));

         Map<String, String> headers = new HashMap<>();
         headers.put("Accept-Encoding", "gzip, deflate, br");
         headers.put("Content-Type", "application/json;charset=UTF-8");
         headers.put("Connection", "keep-alive");
         headers.put("Authorization", "Bearer " + fetchApiToken(headers));

         Request request = Request.RequestBuilder.create()
            .setUrl(url)
            .setHeaders(headers)
            .build();

         Response response = dataFetcher.get(session, request);

         json = CrawlerUtils.stringToJson(response.getBody());
      }

      return json;
   }

   protected String fetchApiToken(Map<String, String> headers) {
      String apiToken = "";

      JSONObject payload = new JSONObject();
      payload.put("client_id", getClientId());
      payload.put("client_secret", getClientSecret());
      payload.put("grant_type", "client_credentials");

      Request request = Request.RequestBuilder.create()
         .setUrl("https://www.merconnect.com.br/oauth/token")
         .setHeaders(headers)
         .setPayload(payload.toString())
         .build();

      JSONObject json = CrawlerUtils.stringToJson(dataFetcher.post(session, request).getBody());

      if (json != null && !json.isEmpty()) {
         apiToken = json.optString("access_token");
      }

      return apiToken;
   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      super.extractInformation(json);
      List<Product> products = new ArrayList<>();

      JSONObject jsonSku = json.optJSONObject("item");

      if (Objects.nonNull(jsonSku) && !jsonSku.isEmpty()) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = scrapInternalId(jsonSku);
         String internalPid = internalId;
         String name = scrapProductName(jsonSku);
         String primaryImage = scrapPrimaryImage(jsonSku);

         String ean = scrapEan(jsonSku);
         List<String> eans = new ArrayList<>();
         eans.add(ean);

         Integer stock = scrapStock(jsonSku);
         Offers offers = stock > 0 ? scrapOffers(jsonSku) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setStock(stock)
            .setEans(eans)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   protected String scrapInternalId(JSONObject json) {
      return json.optString("id");
   }

   protected String scrapProductName(JSONObject json) {
      return json.optString("description");
   }

   protected String scrapPrimaryImage(JSONObject json) {
      return json.optString("image");
   }

   protected Integer scrapStock(JSONObject json) {
      Double stock = json.optDouble("stock");
      return stock != null ? stock.intValue() : 0;
   }

   protected String scrapEan(JSONObject json) {
      return json.optString("bar_code");
   }

   protected Offers scrapOffers(JSONObject json) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();

      Pricing pricing = scrapPricing(json);
      List<String> sales = new ArrayList<>();
      String salesStr = CrawlerUtils.calculateSales(pricing);
      if (salesStr != null) {
         sales.add(salesStr);
      }

      Offer offer = new Offer.OfferBuilder()
         .setSellerFullName(getSellerName())
         .setInternalSellerId(json.optString("market_id", null))
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setPricing(pricing)
         .setIsMainRetailer(true)
         .setSales(sales)
         .build();
      offers.add(offer);

      return offers;
   }

   public static Pricing scrapPricing(JSONObject jsonSku) throws MalformedPricingException {
      Double priceFrom = jsonSku.optDouble("original_price", 0D);
      Double spotlightPrice = jsonSku.optDouble("price", 0D);

      if (spotlightPrice == 0D || spotlightPrice.equals(priceFrom)) {
         spotlightPrice = priceFrom;
         priceFrom = null;
      }
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .build();
   }

   public static CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      Set<Card> cards = Sets.newHashSet(
         Card.VISA,
         Card.MASTERCARD,
         Card.DINERS,
         Card.AMEX,
         Card.ELO,
         Card.SHOP_CARD
      );

      for (Card card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card.toString())
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }


}
