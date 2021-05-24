package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.Card;
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

public class SuperlagoaCrawler extends Crawler {

   private static final String MAINSELLER = "Super Lagoa";
   private String storeId;

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.HIPERCARD.toString(), Card.ELO.toString());


   public SuperlagoaCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.JSOUP);
   }

   public String getStoreId() {
      return storeId;
   }

   public void setStoreId(String storeId) {
      this.storeId = storeId;
   }

   @Override
   protected JSONObject fetch() {
      String id = CrawlerUtils.getStringBetween(session.getOriginalURL(), "produto/", "origin").replace("?", "");
      String url = "https://www.merconnect.com.br/mapp/v2/markets/" + storeId + "/items/" + id;

      String token = getToken();

      Map<String, String> headers = new HashMap<>();
      headers.put("Authorization", "Bearer " + token);

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build();
      String content = this.dataFetcher
         .get(session, request)
         .getBody();

      return CrawlerUtils.stringToJson(content);
   }

   protected String getToken() {
      String url = "https://www.merconnect.com.br/oauth/token";

      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json;charset=UTF-8");
      String payload = "{\"client_id\":\"dcdbcf6fdb36412bf96d4b1b4ca8275de57c2076cb9b88e27dc7901e8752cdff\",\"client_secret\":\"27c92c098d3f4b91b8cb1a0d98138b43668c89d677b70bed397e6a5e0971257c\",\"grant_type\":\"client_credentials\"}";

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload)
         .build();
      String content = this.dataFetcher
         .post(session, request)
         .getBody();


      JSONObject json = CrawlerUtils.stringToJson(content);

      return json.optString("access_token");
   }


   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      super.extractInformation(json);
      List<Product> products = new ArrayList<>();

      if (json.has("item")) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         JSONObject jsonItem = json.optJSONObject("item");

         String internalId = JSONUtils.getIntegerValueFromJSON(jsonItem, "product_id", 0).toString();
         String internalPid = JSONUtils.getIntegerValueFromJSON(jsonItem, "id", 0).toString();
         String name = jsonItem.optString("short_description");
         String description = jsonItem.optString("description");
         String primaryImage = jsonItem.optString("image");
         int stock = jsonItem.optInt("stock");
         List<String> eans = new ArrayList<>();
         eans.add(jsonItem.optString("bar_code"));

         boolean available = stock > 0;
         Offers offers = available ? scrapOffer(jsonItem) : new Offers();

         Product product = ProductBuilder
            .create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setDescription(description)
            .setPrimaryImage(primaryImage)
            .setOffers(offers)
            .setEans(eans)
            .build();

         products.add(product);


      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }


   private Offers scrapOffer(JSONObject jsonObject) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(jsonObject);
      List<String> sales = scrapSales(pricing);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setMainPagePosition(1)
         .setSellerFullName(MAINSELLER)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setSales(sales)
         .setPricing(pricing)
         .build());


      return offers;

   }

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();
      sales.add(CrawlerUtils.calculateSales(pricing));

      return sales;
   }


   private Pricing scrapPricing(JSONObject jsonObject) throws MalformedPricingException {

      Double spotlightPrice = JSONUtils.getValueRecursive(jsonObject, "price", Double.class);

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();

   }


   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
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
