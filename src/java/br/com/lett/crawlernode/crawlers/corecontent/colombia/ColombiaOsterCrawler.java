package br.com.lett.crawlernode.crawlers.corecontent.colombia;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper;
import models.RatingsReviews;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.Collections;
import java.util.List;

public class ColombiaOsterCrawler extends VTEXOldScraper {

   public ColombiaOsterCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return "https://www.ostercolombia.com/";
   }

   @Override
   protected List<String> getMainSellersNames() {
      return Collections.singletonList("Oster Colombia");
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }
}
