package br.com.lett.crawlernode.crawlers.corecontent.fortaleza;

import java.util.*;

import org.json.JSONObject;
import com.google.common.collect.Sets;
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
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer.OfferBuilder;
import models.Offers;
import models.pricing.BankSlip;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;

public class FortalezaObomvizinhoCrawler extends Crawler {


   public FortalezaObomvizinhoCrawler(Session session) {
      super(session);
   }

   private static final String HOME_PAGE = "https://loja.obomvizinho.com.br/loja/";

   private static final String SELLER_FULL_NAME = "obomvizinho";

   // 34 -> CEP = 60840285
   private final String storeId = session.getOptions().optString("storeId");

   public String getStoreId() {
      return storeId;
   }

   protected Set<Card> cards = Sets.newHashSet(Card.VISA, Card.MASTERCARD, Card.AURA, Card.DINERS, Card.HIPER, Card.AMEX);

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE + getStoreId()));
   }

   String getProductIdFromUrl() {
      String url = session.getOriginalURL();
      int lastIndex = url.lastIndexOf("/") + 1;

      return url.substring(lastIndex);
   }

   String getStoreIdFromUrl() {
      String url = session.getOriginalURL();

      int firstIndex = url.lastIndexOf("loja/")+5;
      int lastIndex = url.lastIndexOf("/categoria");

      if (firstIndex < lastIndex) {
         return url.substring(firstIndex, lastIndex);
      } else {
         return "";
      }
   }

   public static Map<String, String> getHeaders() {

      Map<String, String> headers = new HashMap<>();
      headers.put("Auth-Token", "RUsycjRnU1BLTndkblIyTnF1T3FvMGlnUDJKVWx4Nk95eC9IL0RaMU80dz0tLVl3dlBqUjJnK1p2amdheW9WRVlWM0E9PQ");
      headers.put("Connection", "keep-alive");
      headers.put("Accept", "*/*");
      headers.put("Origin", "https://loja.obomvizinho.com.br");
      headers.put("Referer", "https://loja.obomvizinho.com.br/");
      headers.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_1_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Safari/537.36");

      return headers;
   }

   @Override
   protected JSONObject fetch() {

      if (!getStoreIdFromUrl().equals(getStoreId())) {
         return new JSONObject();
      }

      String url = "https://www.merconnect.com.br/mapp/v1/markets/" + getStoreId() + "/items/" + getProductIdFromUrl();

      Request request = RequestBuilder.create()
         .setUrl(url)
         .setHeaders(getHeaders())
         .build();

      String content = this.dataFetcher.get(session, request).getBody();

      return CrawlerUtils.stringToJson(content);
   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {

      List<Product> products = new ArrayList<>();

      JSONObject productJson = JSONUtils.getValueRecursive(json, "item", JSONObject.class);
      if (productJson != null) {

         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = productJson.optString("id");
         String internalPid = productJson.optString("mix_id");
         String name = productJson.optString("description");
         String primaryImage = productJson.optString("image");
         String description = productJson.optString("short_description");

         int stock = productJson.optInt("stock");

         boolean available = stock > 0;

         Offers offers = available ? scrapOffers(productJson) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setDescription(description)
            .setStock(stock)
            .setOffers(offers)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }


   private Offers scrapOffers(JSONObject jsonInfo) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();

      Pricing pricing = scrapPricing(jsonInfo);

      offers.add(OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(Collections.emptyList())
         .build());

      return offers;
   }


   private Pricing scrapPricing(JSONObject jsonInfo) throws MalformedPricingException {
      Double spotlightPrice = jsonInfo.optDouble("price");
      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      return PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();

   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      installments.add(InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      for (Card card : cards) {
         creditCards.add(CreditCardBuilder.create()
            .setBrand(card.toString())
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }
}
