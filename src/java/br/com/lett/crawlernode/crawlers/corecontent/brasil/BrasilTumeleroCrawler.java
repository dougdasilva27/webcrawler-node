package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Parser;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.TrustvoxRatingCrawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXScraper;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import models.RatingsReviews;
import models.pricing.BankSlip;
import models.pricing.CreditCards;
import models.pricing.Pricing;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class BrasilTumeleroCrawler extends VTEXScraper {

   private static final String HOME_PAGE = "https://www.tumelero.com.br/";
   private static final String SELLER_NAME = "tumelero";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.DINERS.toString(), Card.ELO.toString(), Card.AMEX.toString());

   public BrasilTumeleroCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.HTML);
   }

   @Override
   protected Response fetchResponse() {
      if (!session.getOriginalURL().contains("https://")) {
         session.setOriginalURL(session.getOriginalURL().replace("www", "https://www"));
      }
      return super.fetchResponse();
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected List<String> getMainSellersNames() {
      return Arrays.asList(SELLER_NAME);
   }

   @Override
   protected String scrapPidFromApi(Document doc) {
      String internalPid = "";
      String[] urlArray = session.getOriginalURL().split("=");


      if (urlArray.length > 0) {
         internalPid = CommonMethods.getLast(urlArray);
      }

      return internalPid;
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      TrustvoxRatingCrawler trustVox = new TrustvoxRatingCrawler(session, "73909", logger);
      return trustVox.extractRatingAndReviews(internalId, doc, dataFetcher);
   }

//   @Override
//   protected Pricing scrapPricing(Document doc, String internalId, JSONObject comertial, JSONObject discountsJson) throws MalformedPricingException {
//      Double spotlightPrice = comertial.optDouble("Price");
//      Double priceFrom = comertial.optDouble("ListPrice");
//
//
//      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
//         .setFinalPrice(spotlightPrice)
//         .setOnPageDiscount(0.0)
//         .build();
//      if (spotlightPrice != null && spotlightPrice.equals(priceFrom)) {
//         priceFrom = null;
//      }
//
//      return Pricing.PricingBuilder.create()
//         .setSpotlightPrice(spotlightPrice)
//         .setPriceFrom(priceFrom)
//         .setBankSlip(bankSlip)
//         .build();
//   }

   @Override
   protected Pricing scrapPricing(Document doc, String internalId, JSONObject comertial, JSONObject discountsJson, JSONObject jsonSku) throws MalformedPricingException {
      Double principalPrice = comertial.optDouble("Price");
      Double priceFrom = comertial.optDouble("ListPrice");

      if (jsonSku.optString("measurementUnit").equals("kg")) {
         Double unitMultiplier = jsonSku.optDouble("unitMultiplier");
         principalPrice = Math.floor((principalPrice * unitMultiplier) * 100) / 100.0;
         if (priceFrom != null) priceFrom = Math.floor((priceFrom * unitMultiplier) * 100) / 100.0;
      }

      CreditCards creditCards = CrawlerUtils.scrapCreditCards(principalPrice,cards);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(principalPrice)
         .setOnPageDiscount(0.0)
         .build();

      Double spotlightPrice = scrapSpotlightPrice(doc, internalId, principalPrice, comertial, discountsJson);
      if (priceFrom != null && spotlightPrice != null && spotlightPrice.equals(priceFrom)) {
         priceFrom = null;
      }

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();
   }


}
