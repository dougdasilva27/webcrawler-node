package br.com.lett.crawlernode.crawlers.corecontent.fortaleza;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import com.google.common.collect.Sets;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
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

public class FortalezaCenterboxCrawler extends Crawler {
   private static final String HOME_PAGE = "https://www.merconnect.com.br/api/v2/";
   private static final String CEP = "60192-105";
   private static final String SELLER_FULL_NAME = "centerbox";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public FortalezaCenterboxCrawler(Session session) {
      super(session);

   }


   @Override
   protected Object fetch() {
      JSONObject api = new JSONObject();

      Map<String, String> headers = new HashMap<>();
      headers.put("Auth-Token", "RUsycjRnU1BLTndkblIyTnF1T3FvMGlnUDJKVWx4Nk95eC9IL0RaMU80dz0tLVl3dlBqUjJnK1p2amdheW9WRVlWM0E9PQ");
      headers.put("Connection", "keep-alive");
      String url = "https://www.merconnect.com.br/api/v4/markets?cep=" + CEP + "&neighborhood_id=42&market_codename=centerbox";

      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).build();
      String content = this.dataFetcher.get(session, request).getBody();

      if (content == null || content.isEmpty()) {
         content = new ApacheDataFetcher().get(session, request).getBody();
      }

      api = CrawlerUtils.stringToJson(content);

      return api;
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   private JSONArray getProductInfo() {
      JSONArray api = new JSONArray();

      Request request = RequestBuilder.create().setUrl(session.getOriginalURL()).setCookies(cookies).build();
      String content = this.dataFetcher.get(session, request).getBody();

      JSONObject jsonInfo = CrawlerUtils.stringToJson(content);

      api = JSONUtils.getJSONArrayValue(jsonInfo, "mixes");

      return api;
   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      super.extractInformation(json);
      List<Product> products = new ArrayList<>();

      if (isProductPage(session.getOriginalURL())) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         JSONArray arr = getProductInfo();

         for (Object arrayOfArrays : arr) {

            if (arrayOfArrays != null) {

               JSONArray array = (JSONArray) arrayOfArrays;

               for (Object productJsonInfo : array) {

                  JSONObject jsonInfo = (JSONObject) productJsonInfo;

                  if (jsonInfo != null) {

                     String internalId = jsonInfo.optString("id");
                     String internalPid = jsonInfo.optString("mix_id");
                     String name = jsonInfo.optString("description");
                     Integer stock = jsonInfo.optInt("stock");
                     boolean available = stock != null && stock > 0;
                     String primaryImage = jsonInfo.optString("image");
                     String description = jsonInfo.optString("short_description");
                     Offers offers = available ? scrapOffers(jsonInfo) : new Offers();

                     // Creating the product
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


                  }
               }
            }
         }
      } else

      {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private boolean isProductPage(String url) {
      return url.contains("search");
   }

   private Offers scrapOffers(JSONObject jsonInfo) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(jsonInfo);
      List<String> sales = new ArrayList<>();

      offers.add(OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(SELLER_FULL_NAME)
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .setSales(sales)
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
      if (installments.getInstallments().isEmpty()) {
         installments.add(InstallmentBuilder.create()
               .setInstallmentNumber(1)
               .setInstallmentPrice(spotlightPrice)
               .build());
      }

      for (String card : cards) {
         creditCards.add(CreditCardBuilder.create()
               .setBrand(card)
               .setInstallments(installments)
               .setIsShopCard(false)
               .build());
      }

      return creditCards;
   }

}
