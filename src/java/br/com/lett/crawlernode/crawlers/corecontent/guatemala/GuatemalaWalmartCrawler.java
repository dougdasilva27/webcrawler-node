package br.com.lett.crawlernode.crawlers.corecontent.guatemala;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXNewScraper;
import models.RatingsReviews;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.Arrays;
import java.util.List;

public class GuatemalaWalmartCrawler extends VTEXNewScraper {
   public GuatemalaWalmartCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return "https://www.walmart.com.gt/";
   }

   @Override
   protected List<String> getMainSellersNames() {
      return Arrays.asList("Walmart GTM");
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }
}
