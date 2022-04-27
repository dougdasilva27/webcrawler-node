package br.com.lett.crawlernode.crawlers.corecontent.curitiba;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.MathUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class CuritibaNicheleCrawler extends VTEXOldScraper {

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public CuritibaNicheleCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return "https://www.nichele.com.br/";
   }

   @Override
   protected List<String> getMainSellersNames() {
      return Arrays.asList("DEPOSITO DE MATERIAIS PARA CONSTRUCAO NICHELE LTDA");
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
               String nameLow = name.toLowerCase();
               if(nameLow !=null){
                  Boolean boleto = nameLow.contains("boleto");
                  if (boleto != null & boleto ) {
                     if (paymentDiscount != null) {
                        discount = paymentDiscount.optDouble("discount");
                        bankSlipPrice = MathUtils.normalizeTwoDecimalPlaces(bankSlipPrice - (bankSlipPrice * discount));
                     }
                     break;
                  }
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

      Installments installments = new Installments();
      if (installments.getInstallments().isEmpty()) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(comertial.optDouble("Price"))
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
