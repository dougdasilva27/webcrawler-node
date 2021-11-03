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
   protected Pricing scrapPricing(Document doc, String internalId, JSONObject comertial, JSONObject discountsJson) throws MalformedPricingException {
      Double principalPrice = comertial.optDouble("Price");
      Double priceFrom = comertial.optDouble("ListPrice");

      CreditCards creditCards = scrapCreditCards(comertial, discountsJson, true);

      Double spotlightPrice = scrapSpotlightPrice(doc, internalId, principalPrice, comertial, discountsJson);
      if (priceFrom != null && spotlightPrice != null && spotlightPrice.equals(priceFrom)) {
         priceFrom = null;
      }
      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
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
