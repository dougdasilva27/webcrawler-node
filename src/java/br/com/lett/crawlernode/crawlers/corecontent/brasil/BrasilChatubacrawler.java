package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldNewImpl;
import br.com.lett.crawlernode.util.MathUtils;
import exceptions.MalformedPricingException;
import models.pricing.*;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

public class BrasilChatubacrawler extends VTEXOldNewImpl {
   public BrasilChatubacrawler(@NotNull Session session) {
      super(session);
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

   @Override
   protected CreditCards scrapCreditCards(JSONObject comertial, JSONObject discounts, boolean mustSetDiscount) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      JSONObject paymentOptions = comertial.optJSONObject("PaymentOptions");
      Double price = comertial.optDouble("Price");
      if (paymentOptions != null) {
         JSONArray cardsArray = paymentOptions.optJSONArray("installmentOptions");
         if (cardsArray != null) {
            for (Object o : cardsArray) {
               JSONObject cardJson = (JSONObject) o;

               String paymentCode = cardJson.optString("paymentSystem");
               JSONObject cardDiscount = discounts.has(paymentCode) ? discounts.optJSONObject(paymentCode) : null;
               String paymentName = cardJson.optString("paymentName");
               JSONArray installmentsArray = cardJson.optJSONArray("installments");

               if (!installmentsArray.isEmpty() && !paymentName.toLowerCase().contains("boleto")) {
                  Installments installments = new Installments();

                  for (Object object : installmentsArray) {
                     JSONObject installmentJson = (JSONObject) object;

                     Integer installmentNumber = installmentJson.optInt("count");
                     Double discount = 0d;
                     Double totalValue = price;
                     Double value = price / installmentNumber;
                     Double interest = installmentJson.optDouble("interestRate");

                     if (cardDiscount != null) {
                        int minInstallment = cardDiscount.optInt("minInstallment");
                        int maxInstallment = cardDiscount.optInt("maxInstallment");

                        if (installmentNumber >= minInstallment && installmentNumber <= maxInstallment) {
                           discount = cardDiscount.optDouble("discount");
                           value = MathUtils.normalizeTwoDecimalPlaces(value - (value * discount));
                           totalValue = MathUtils.normalizeTwoDecimalPlaces(installmentNumber * value);
                        }
                     }

                     installments.add(setInstallment(installmentNumber, value, interest, totalValue, mustSetDiscount ? discount : null));
                  }

                  String cardBrand = null;
                  for (Card card : Card.values()) {
                     if (card.toString().toLowerCase().contains(paymentName.toLowerCase())) {
                        cardBrand = card.toString();
                        break;
                     }
                  }

                  boolean isShopCard = false;
                  if (cardBrand == null) {
                     for (String sellerName : mainSellersNames) {
                        if ((storeCard != null && paymentName.equalsIgnoreCase(storeCard)) ||
                           paymentName.toLowerCase().contains(sellerName.toLowerCase())) {
                           isShopCard = true;
                           cardBrand = paymentName;
                           break;
                        }
                     }
                  }

                  if (cardBrand != null) {
                     creditCards.add(CreditCard.CreditCardBuilder.create()
                        .setBrand(cardBrand)
                        .setInstallments(installments)
                        .setIsShopCard(isShopCard)
                        .build());
                  }
               }
            }
         }
      }

      return creditCards;
   }


}
