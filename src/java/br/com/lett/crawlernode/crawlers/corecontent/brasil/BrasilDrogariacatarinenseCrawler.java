package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXNewScraper;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper;
import models.RatingsReviews;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.Arrays;
import java.util.List;

public class BrasilDrogariacatarinenseCrawler extends VTEXOldScraper {
   public BrasilDrogariacatarinenseCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return "https://www.drogariacatarinense.com.br/";
   }

   @Override
   protected List<String> getMainSellersNames() {
      return Arrays.asList("CIA LATINO AMERICANA DE MEDICAMENTOS.");
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }
}
