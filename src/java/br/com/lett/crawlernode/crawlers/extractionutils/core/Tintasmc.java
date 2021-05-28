package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
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

public abstract class Tintasmc extends Crawler {

   private static final String SELLER_FULL_NAME = "TintasMC Brasil";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   protected Tintasmc(final Session session) {
      super(session);
      super.config.setFetcher(FetchMode.FETCHER);
   }

   /**
    * Can be found after insert address on the website. Request: 'https://www.obrazul.com.br/api/search/products-v2'
    *
    * Format:
    * {"address":{"country":"BR","_country":"Brasil","address":"","city":"","_state":"","neighborhood":"","state":"","postal_code":""},"lng":"","full_address":"","lat":"","place_id":""}
    *
    * @return location data in json format
    */
   protected abstract String setJsonLocation();

   private String buildURLToRequest() {
      String protocolApi = "https://www.obrazul.com.br/api/search/products-v2/?user_location=";

      String locationJson = setJsonLocation();

      String locationString = CommonMethods.encondeStringURLToISO8859(locationJson, logger, session);
      String slug = session.getOriginalURL().split("produtos/")[1];

      return protocolApi + locationString + "&slug=" + slug;
   }

   @Override
   protected Object fetch() {
      String url = buildURLToRequest();

      Map<String, String> headers = new HashMap<>();
      headers.put("authorization", "token 70f16006cb76009ad4d6448910360b5ff55eb9e1"); // This is not the best way to set the token but when this scraper was created this was the only way
      // found...

      Request request = Request.RequestBuilder.create().setHeaders(headers).setUrl(url).build();
      String content = this.dataFetcher.get(session, request).getBody();
      JSONObject jsonObject = CrawlerUtils.stringToJson(content);

      return JSONUtils.getJSONArrayValue(jsonObject, "products");
   }

   @Override
   public List<Product> extractInformation(JSONArray productsArr) throws Exception {
      super.extractInformation(productsArr);
      List<Product> products = new ArrayList<>();

      if (session.getOriginalURL().contains("produtos")) {

         for (Object p : productsArr) {
            JSONObject prod = (JSONObject) p;

            String internalId = scrapInternalId(prod);
            String name = prod.optString("fullname");
            String primaryImage = prod.optString("image");
            String description = prod.optString("short_description");
            Offers offers = scrapOffer(prod);

            // Creating the product
            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalId)
               .setName(name)
               .setPrimaryImage(primaryImage)
               .setDescription(description)
               .setOffers(offers)
               .build();

            products.add(product);

         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }
      return products;
   }

   private String scrapInternalId(JSONObject prod) {
      String internalId = "";
      JSONArray stores = JSONUtils.getJSONArrayValue(prod, "stores");

      if (!stores.isEmpty()) {
         for (Object arr : stores) {
            JSONObject store = (JSONObject) arr;
            internalId = store.optString("productstore_id");
         }
      }
      return internalId;
   }

   private Offers scrapOffer(JSONObject prod) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      JSONArray stores = JSONUtils.getJSONArrayValue(prod, "stores");

      for (Object arr : stores) {
         JSONObject store = (JSONObject) arr;

         Pricing pricing = scrapPricing(store);
         List<String> sales = new ArrayList<>();

         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(SELLER_FULL_NAME)
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .setSales(sales)
            .build());
      }
      return offers;

   }

   private Pricing scrapPricing(JSONObject store) throws MalformedPricingException {
      Double spotlightPrice = store.optDouble("price");
      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
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
}
