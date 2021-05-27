
package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper;
import models.RatingsReviews;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.Collections;
import java.util.List;

public class BrasilLojacomerbemCrawler extends VTEXOldScraper {

   private static final String HOME_PAGE = "https://www.lojacomerbem.com.br/";
   private static final List<String> SELLERS = Collections.singletonList("LOJA COMER BEM");

   public BrasilLojacomerbemCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
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
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject apiJson) {
      return null;
   }
}
