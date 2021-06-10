package br.com.lett.crawlernode.crawlers.corecontent.colombia;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper;
import models.RatingsReviews;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.Arrays;
import java.util.List;

public class ColombiaTiendahogaruniversalCrawler extends VTEXOldScraper {

   public ColombiaTiendahogaruniversalCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return "https://www.tiendahogaruniversal.com/";
   }

   @Override
   protected List<String> getMainSellersNames() {
      return Arrays.asList("tiendahogaruniversal");
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }


}
