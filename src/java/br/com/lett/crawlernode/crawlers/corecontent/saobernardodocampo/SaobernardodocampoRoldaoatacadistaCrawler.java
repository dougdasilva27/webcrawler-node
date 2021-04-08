package br.com.lett.crawlernode.crawlers.corecontent.saobernardodocampo;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
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
import org.jsoup.nodes.Document;

import java.util.*;

public class SaobernardodocampoRoldaoatacadistaCrawler extends Crawler {

   private static final String SELLER_NAME = "Roldao Atacadista";
   private static final String STORE_ID = "34";
   private String token;

   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString());

   public SaobernardodocampoRoldaoatacadistaCrawler(Session session) {
      super(session);
      config.setFetcher(FetchMode.JSOUP);

   }

   private JSONObject requestLogin() {

      String url = "https://api.roldao.com.br/api/public/anonymous-client";

      Request request = RequestBuilder.create()
         .setPayload("{}")
         .setUrl(url)
         .build();

      String content = this.dataFetcher
         .post(session, request)
         .getBody();

      return CrawlerUtils.stringToJson(content);
   }


   private JSONObject requestToken() {

      String url = "https://api.roldao.com.br/api/public/oauth/access-token";
      JSONObject json = requestLogin();
      String username = json.optString("user");
      String password = json.getString("password");

      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/x-www-form-urlencoded");

      String payload = "username=" + username + "&password=" + password + "&client_id=KFxk0cvYvtsKMe9fCpjxucUFwTQW4ZZy&client_secret=UBFfD3mFNT6EOpT7hSGFILk2MDA72Dq9b0wmYUPJq7yDg1kLcnWGhh7JahKka4XY&grant_type=password";

      Request request = RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload)
         .build();

      String content = this.dataFetcher
         .post(session, request)
         .getBody();

      return CrawlerUtils.stringToJson(content);

   }


   private JSONObject getCardId() {
      JSONObject json = requestToken();
      token = "Bearer " + json.optString("access_token");
      String url = "https://api.roldao.com.br/api/shopping-cart";

      Map<String, String> headers = new HashMap<>();
      headers.put("X-Authorization", token);
      headers.put("Referer", "https://www.roldao.com.br/");
      headers.put("Content-Type", "application/json; charset=UTF-8");
      headers.put("Accept", "*/*");
      headers.put("Accept-Encoding", "gzip, deflate, br");

      String zipCode = "9695000";
      Request request = RequestBuilder.create()
         .setPayload("{\"type\":\"pickup\",\"zipcode\":" + zipCode + ",\"branchId\":" + STORE_ID + "}")
         .setUrl(url)
         .setHeaders(headers)
         .build();

      String content = this.dataFetcher
         .post(session, request)
         .getBody();

      return CrawlerUtils.stringToJson(content);
   }

   private JSONObject getStatusOnStore(String internalId) {

      JSONObject jsonObject = getCardId();
      String cardId = jsonObject.optString("shoppingCartId");

      Map<String, String> headers = new HashMap<>();
      headers.put("X-Authorization", token);
      headers.put("Referer", "https://www.roldao.com.br/");
      headers.put("Content-Type", "application/json; charset=UTF-8");
      headers.put("Accept", "*/*");
      headers.put("Accept-Encoding", "gzip, deflate, br");

      String payload = "{\"quantity\":1,\"sku\":\"" + internalId + "\"}";

      String urlApi = "https://api.roldao.com.br/api/shopping-cart/" + cardId + "/item";

      Request request = Request.RequestBuilder.create()
         .setUrl(urlApi)
         .setHeaders(headers)
         .setPayload(payload)
         .build();
      String content = this.dataFetcher
         .post(session, request)
         .getBody();

      return CrawlerUtils.stringToJson(content);

   }


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-code", true);
         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".image-gallery-image", "alt").split("_")[0];
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-title", true);
         //site hasn't category
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".image-gallery-image", Arrays.asList("src"), "https", "api.roldao.com.br");
         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, ".image-gallery-image", Arrays.asList("src"), "https", "api.roldao.com.br", primaryImage);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".product-description-mobile span"));
         JSONObject statusOnStore = getStatusOnStore(internalId);
         boolean available = statusOnStore != null && !statusOnStore.has("error");
         Offers offers = available ? scrapOffers(doc, statusOnStore) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".ProductDetailsContainer") != null;
   }

   private Offers scrapOffers(Document doc, JSONObject statusOnStore) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(statusOnStore);
      List<String> sales = scrapSales(doc);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;

   }

   private List<String> scrapSales(Document doc) {
      List<String> sales = new ArrayList<>();

      StringBuilder stringBuilder = new StringBuilder();
      String qty = CrawlerUtils.scrapStringSimpleInfo(doc, ".BannerProgressiveDiscountComponent .top", true);
      String price = CrawlerUtils.scrapStringSimpleInfo(doc, ".BannerProgressiveDiscountComponent .bottom", false);

      if (qty != null && price != null) {
         stringBuilder.append(qty + " ");
         stringBuilder.append(price);
         sales.add(stringBuilder.toString());
      }

      return sales;
   }

   private Pricing scrapPricing(JSONObject statusOnStore) throws MalformedPricingException {
      Double spotlightPrice = JSONUtils.getValueRecursive(statusOnStore, "items.0.price", Double.class);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      //Site hasn't any product with old price

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

   //Site hasn't rating


}
