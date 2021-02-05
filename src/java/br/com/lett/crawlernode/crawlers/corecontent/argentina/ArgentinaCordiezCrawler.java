package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper;
import models.RatingsReviews;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.Collections;
import java.util.List;

public class ArgentinaCordiezCrawler extends VTEXOldScraper {

   private static final String HOME_PAGE = "https://www.cordiez.com.ar/";
   private static final List<String> MAIN_SELLERS = Collections.singletonList("C.Y.R.E. S.A.");

   public ArgentinaCordiezCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected List<String> getMainSellersNames() {
      return MAIN_SELLERS;
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }
}
