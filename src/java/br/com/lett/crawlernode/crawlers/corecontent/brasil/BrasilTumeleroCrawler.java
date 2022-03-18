package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Parser;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.TrustvoxRatingCrawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXScraper;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import exceptions.MalformedPricingException;
import models.RatingsReviews;
import models.pricing.BankSlip;
import models.pricing.CreditCards;
import models.pricing.Pricing;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.Arrays;
import java.util.List;

public class BrasilTumeleroCrawler extends VTEXScraper {

   private static final String HOME_PAGE = "https://www.tumelero.com.br/";
   private static final String SELLER_NAME = "tumelero";

   public BrasilTumeleroCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.HTML);
   }

   @Override
   protected Response fetchResponse() {
      if (!session.getOriginalURL().contains("https://")){
         session.setOriginalURL(session.getOriginalURL().replace("www","https://www"));
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


      if (urlArray.length > 0){
         internalPid = CommonMethods.getLast(urlArray);
      }

      return internalPid;
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      TrustvoxRatingCrawler trustVox = new TrustvoxRatingCrawler(session, "73909", logger);
      return trustVox.extractRatingAndReviews(internalId, doc, dataFetcher);
   }

   @Override
   protected Pricing scrapPricing(Document doc, String internalId, JSONObject comertial, JSONObject discountsJson) throws MalformedPricingException {
      Double spotlightPrice = comertial.optDouble("Price");
      Double priceFrom = comertial.optDouble("ListPrice");


      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, 0d);
      if (spotlightPrice != null && spotlightPrice.equals(priceFrom)) {
         priceFrom = null;
      }

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setBankSlip(bankSlip)
         .build();
   }
}
