package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXNewScraper;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper;
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


public class BrasilBalarotiCrawler extends VTEXOldScraper {

   private static final String HOME_PAGE = "https://www.balaroti.com.br//";
   private static final List<String> SELLERS = Arrays.asList( "balaroti","balaroti comércio de materiais de construção sa");

  public BrasilBalarotiCrawler(Session session) {
    super(session);
  }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected List<String> getMainSellersNames() {
      return SELLERS;
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

      BankSlip bankSlip;

      if(spotlightPrice!=null){
         bankSlip = BankSlip.BankSlipBuilder.create()
            .setFinalPrice(spotlightPrice)
            .build();
      }
      else {
         bankSlip = BankSlip.BankSlipBuilder.create()
            .setFinalPrice(principalPrice)
            .build();
      }



      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();
   }


}
