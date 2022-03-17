package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.MathUtils;
import com.google.common.collect.Sets;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXNewScraper;
import exceptions.MalformedPricingException;
import models.RatingsReviews;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.Pricing.PricingBuilder;


public class BrasilCassolCrawler extends VTEXNewScraper {

   private static final String HOME_PAGE = "https://www.cassol.com.br/";
   private static final String STORE_CARD = "Cartão Cassol";
   private Set<String> cards = Sets.newHashSet(Card.VISA.toString(),
      Card.MASTERCARD.toString(), Card.DINERS.toString(), Card.HIPERCARD.toString(), Card.ELO.toString());

   public BrasilCassolCrawler(Session session) {
      super(session);
      super.storeCard = STORE_CARD;
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected List<String> getMainSellersNames() {
      return Arrays.asList("CASSOL CENTERLAR");
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }

   @Override
   protected BankSlip scrapBankSlip(Double spotlightPrice, JSONObject comertial, JSONObject discounts, boolean mustSetDiscount) throws MalformedPricingException {
      Double bankSlipPrice = spotlightPrice;
      Double discount = 0d;

      JSONObject paymentOptions = comertial.optJSONObject("PaymentOptions");
      if (paymentOptions != null) {
         JSONArray cardsArray = paymentOptions.optJSONArray("installmentOptions");
         if (cardsArray != null) {
            for (Object o : cardsArray) {
               JSONObject paymentJson = (JSONObject) o;

               String paymentCode = paymentJson.optString("paymentSystem");
               JSONObject paymentDiscount = discounts.has(paymentCode) ? discounts.optJSONObject(paymentCode) : null;
               String name = paymentJson.optString("paymentName");

               if (name.toLowerCase().contains("boleto")) {
                  if (paymentDiscount != null) {
                     discount = paymentDiscount.optDouble("discount");
                     bankSlipPrice = MathUtils.normalizeTwoDecimalPlaces(bankSlipPrice - (bankSlipPrice * discount));
                  }

                  break;
               }
            }
         }
      }

      if (!mustSetDiscount) {
         discount = null;
      }

      return BankSlip.BankSlipBuilder.create()
         .setFinalPrice(bankSlipPrice)
         .setOnPageDiscount(discount)
         .build();
   }
   
   protected CreditCards scrapCreditCardsCassol(JSONObject comertial) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      JSONArray cardsArray = comertial.optJSONArray("Installments");
      if (cardsArray != null) {
         for (Object o : cardsArray) {
            JSONObject cardJson = (JSONObject) o;

            Integer installmentNumber = cardJson.optInt("NumberOfInstallments");
            Double value = cardJson.optDouble("Value");
            Double interest = cardJson.optDouble("InterestRate");
            if (installments.getInstallmentPrice(1) != null && installments.getInstallmentPrice(1).equals(value)) {
               break;
            }
            installments.add(setInstallment(installmentNumber, value, interest, null, null));
         }

         for (String card : cards) {
            creditCards.add(CreditCard.CreditCardBuilder.create()
               .setBrand(card)
               .setInstallments(installments)
               .setIsShopCard(false)
               .build());

         }
      }

      return creditCards;

   }

   @Override
   protected String scrapDescription(Document doc, JSONObject productJson) {
      StringBuilder description = new StringBuilder();

      if (productJson.has("description")) {
         description.append("<div>");
         description.append(sanitizeDescription(productJson.optString("description", "")));
         description.append("</div>");
      }

      description.append(scrapSpecsDescriptions(productJson));

      return description.toString();
   }

}
