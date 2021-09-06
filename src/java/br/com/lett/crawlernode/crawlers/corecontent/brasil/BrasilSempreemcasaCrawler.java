package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
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

public class BrasilSempreemcasaCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "sempre em casa";
   protected String lat;
   protected String longi;

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());


   public BrasilSempreemcasaCrawler(Session session) {
      super(session);
      config.setFetcher(FetchMode.JSOUP);
      lat = session.getOptions().optString("latitude");
      longi = session.getOptions().optString("longitude");
   }

   @Override
   protected JSONObject fetch() {
      String productSlug = CommonMethods.getLast(session.getOriginalURL().split("/"));

      if(!productSlug.contains("latitude") && !productSlug.contains("longitude")){
         productSlug += "?latitude=" + lat + "&longitude=" + longi;
      }
      String url = "https://api.sempreemcasa.com.br/products/" + productSlug;

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .build();

      Response response = this.dataFetcher.get(session, request);
      return JSONUtils.stringToJson(response.getBody());
   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      super.extractInformation(json);
      List<Product> products = new ArrayList<>();

      if (json.has("id")) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = json.optString("id");
         String primaryImage = json.optString("image");
         String name = json.optString("name");
         String description = json.optString("description");
         JSONArray variations = json.optJSONArray("packs");

         for (Object o : variations) {
            JSONObject variation = (JSONObject) o;

            String internalId = internalPid + "-" + variation.optString("id");
            int qtd = variation.optInt("quantity");
            String variationName = name + " - " + qtd;

            Offers offer = scrapOffer(variation);

            // Creating the product
            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(variationName)
               .setDescription(description)
               .setPrimaryImage(primaryImage)
               .setOffers(offer)
               .build();

            products.add(product);
         }

         //Capturing the unit price if it's in the page
         if(!variations.isEmpty()){
            String internalId = internalPid + "-1";
            String variationName = name + " - unidade";
            Offers offer = scrapUnitOffer(json);

            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(variationName)
               .setDescription(description)
               .setPrimaryImage(primaryImage)
               .setOffers(offer)
               .build();

            products.add(product);
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private Offers scrapOffer(JSONObject json) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      List<String> sales = new ArrayList<>();

      Pricing pricing = scrapPricing(json);

      offers.add(Offer.OfferBuilder.create()
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


   private Pricing scrapPricing(JSONObject json) throws MalformedPricingException {
      Double spotlightPrice = json.optDouble("current_price");
      Double priceFrom = json.optDouble("original_price") != 0d ? json.optDouble("original_price") : null;

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
   }


   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      if (installments.getInstallments().isEmpty()) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());
      }

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   private Offers scrapUnitOffer(JSONObject json) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      List<String> sales = new ArrayList<>();

      Double spotlightPrice = JSONUtils.getValueRecursive(json, "packs.0.current_unity_price", Double.class);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      Pricing pricing = Pricing.PricingBuilder.create()
         .setPriceFrom(null)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();

      offers.add(Offer.OfferBuilder.create()
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
}
