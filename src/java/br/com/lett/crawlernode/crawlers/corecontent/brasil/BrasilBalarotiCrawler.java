package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.*;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXNewScraper;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.MathUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import models.RatingsReviews;
import models.pricing.BankSlip;
import models.pricing.CreditCards;
import models.pricing.Pricing;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXCrawlersUtils;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;


public class BrasilBalarotiCrawler extends VTEXNewScraper {

   private static final String HOME_PAGE = "https://www.balaroti.com.br/";
   private Set<String> cards = Sets.newHashSet(Card.VISA.toString(),
      Card.MASTERCARD.toString(), Card.DINERS.toString(), Card.HIPERCARD.toString(), Card.ELO.toString());

   public BrasilBalarotiCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected List<String> getMainSellersNames() {
      return Arrays.asList("balaroti","balaroti comércio de materiais de construção sa");
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }

   @Override
   protected CreditCards scrapCreditCards(JSONObject comertial, JSONObject discounts, boolean mustSetDiscount) throws MalformedPricingException {
      Double creditCardPrice = JSONUtils.getDoubleValueFromJSON(comertial, "ListPrice", false);
      return CrawlerUtils.scrapCreditCards(creditCardPrice, cards);
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

}
