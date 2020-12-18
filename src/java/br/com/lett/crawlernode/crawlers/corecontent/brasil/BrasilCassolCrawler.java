package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import java.util.Arrays;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXNewScraper;
import br.com.lett.crawlernode.util.CrawlerUtils;
import exceptions.MalformedPricingException;
import models.RatingsReviews;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;

/**
 * date: 27/03/2018
 * 
 * @author gabriel
 *
 */

public class BrasilCassolCrawler extends VTEXNewScraper {

   private static final String HOME_PAGE = "https://www.cassol.com.br/";
   private static final String STORE_CARD = "Cartão Cassol";

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
   protected Pricing scrapPricing(Document doc, String internalId, JSONObject comertial, JSONObject discountsJson) throws MalformedPricingException {
      Pricing pricing = super.scrapPricing(doc, internalId, comertial, discountsJson);

      // when the product price is < 100, this site don't use the normal installments json
      if (pricing.getSpotlightPrice() < 100d) {
         pricing = PricingBuilder.create()
               .setSpotlightPrice(pricing.getSpotlightPrice())
               .setPriceFrom(pricing.getPriceFrom())
               .setBankSlip(CrawlerUtils.setBankSlipOffers(pricing.getSpotlightPrice(), null))
               .setCreditCards(scrapCreditCardsCassol(comertial))
               .build();
      }

      return pricing;
   }

   protected CreditCards scrapCreditCardsCassol(JSONObject comertial) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      JSONArray cardsArray = comertial.optJSONArray("Installments");
      if (cardsArray != null) {
         for (Object o : cardsArray) {
            JSONObject cardJson = (JSONObject) o;

            Installments installments = new Installments();

            String paymentName = cardJson.optString("PaymentSystemName");

            Integer installmentNumber = cardJson.optInt("NumberOfInstallments");
            Double value = cardJson.optDouble("TotalValuePlusInterestRate");
            Double interest = cardJson.optDouble("InterestRate");

            installments.add(setInstallment(installmentNumber, value, interest, null, null));

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
               creditCards.add(CreditCardBuilder.create()
                     .setBrand(cardBrand)
                     .setInstallments(installments)
                     .setIsShopCard(isShopCard)
                     .build());
            }
         }
      }

      return creditCards;
   }

   @Override
   protected String scrapDescription(Document doc, JSONObject productJson) {
      StringBuilder description = new StringBuilder();

      if (productJson.has("description")) {
         description.append("<div>");
         description.append(sanitizeDescription(productJson.get("description")));
         description.append("</div>");
      }

      description.append(scrapSpecsDescriptions(productJson));

      return description.toString();
   }

}
