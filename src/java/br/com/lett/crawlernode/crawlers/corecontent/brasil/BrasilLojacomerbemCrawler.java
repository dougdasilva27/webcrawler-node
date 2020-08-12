
package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.Arrays;
import java.util.List;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXOldScraper;
import models.RatingsReviews;

public class BrasilLojacomerbemCrawler extends VTEXOldScraper {

   private static final String HOME_PAGE = "https://www.lojacomerbem.com.br/";
   private static final List<String> SELLERS = Arrays.asList("SYNAPCOM COMERCIO ELETRONICO LTDA");

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
