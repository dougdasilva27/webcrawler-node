package br.com.lett.crawlernode.crawlers.corecontent.colombia;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper;
import models.RatingsReviews;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.Arrays;
import java.util.List;

public class Flamingo extends VTEXOldScraper {

   public Flamingo(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return "https://www.flamingo.com.co/";
   }

   @Override
   protected List<String> getMainSellersNames() {
      return Arrays.asList("flamingo");
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }

}
