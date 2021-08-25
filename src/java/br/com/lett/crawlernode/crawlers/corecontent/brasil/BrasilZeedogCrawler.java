package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper;
import models.RatingsReviews;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.Arrays;
import java.util.List;

public class BrasilZeedogCrawler extends VTEXOldScraper {
   public BrasilZeedogCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return "https://www.zeedog.com.br/";
   }

   @Override
   protected List<String> getMainSellersNames() {
      return Arrays.asList("Zee.Dog");
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }
}
