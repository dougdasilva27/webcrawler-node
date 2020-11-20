package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXNewScraper;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.MathUtils;
import exceptions.MalformedPricingException;
import models.RatingsReviews;
import models.pricing.BankSlip;
import models.pricing.CreditCards;
import models.pricing.Pricing;

public class BrasilKitchenaidCrawler extends VTEXNewScraper {

   private static final String HOME_PAGE = "https://www.kitchenaid.com.br/";
   private static final String MAIN_SELLER_NAME_LOWER = "Kitchenaid";

   public BrasilKitchenaidCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.APACHE);
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected List<String> getMainSellersNames() {
      return Arrays.asList(MAIN_SELLER_NAME_LOWER);
   }

   @Override
   protected List<String> scrapSales(Document doc, JSONObject offerJson, String internalId, String internalPid, Pricing pricing) {
      String sale = CrawlerUtils.scrapStringSimpleInfo(doc, ".product__flags .price-flag", true);
      return sale != null && !sale.isEmpty() ? Arrays.asList(sale) : new ArrayList<>();
   }


   @Override
   protected Double scrapSpotlightPrice(Document doc, String internalId, Double principalPrice, JSONObject comertial, JSONObject discountsJson) {
      Double spotlightPrice = super.scrapSpotlightPrice(doc, internalId, principalPrice, comertial, discountsJson);
      Double maxDiscount = 0d;
      if (discountsJson != null && discountsJson.length() > 0) {
         for (String key : discountsJson.keySet()) {
            JSONObject paymentEffect = discountsJson.optJSONObject(key);
            Double discount = paymentEffect.optDouble("discount");

            if (discount > maxDiscount) {
               maxDiscount = discount;
            }
         }
      }

      if (maxDiscount > 0d) {
         spotlightPrice = MathUtils.normalizeTwoDecimalPlaces(spotlightPrice - (spotlightPrice * maxDiscount));
      }

      return spotlightPrice;
   }

   @Override
   protected BankSlip scrapBankSlip(Double spotlightPrice, JSONObject comertial, JSONObject discounts, boolean mustSetDiscount) throws MalformedPricingException {
      return super.scrapBankSlip(spotlightPrice, comertial, discounts, false);
   }

   @Override
   protected CreditCards scrapCreditCards(JSONObject comertial, JSONObject discounts, boolean mustSetDiscount) throws MalformedPricingException {
      return super.scrapCreditCards(comertial, discounts, false);
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }

}
