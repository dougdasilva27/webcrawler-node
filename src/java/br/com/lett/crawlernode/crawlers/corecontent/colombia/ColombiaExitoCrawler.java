package br.com.lett.crawlernode.crawlers.corecontent.colombia;

import java.util.Arrays;
import java.util.List;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXNewScraper;
import models.RatingsReviews;



public class ColombiaExitoCrawler extends VTEXNewScraper {
   private static final String HOME_PAGE = "https://www.exito.com/";
   private static final String MAIN_SELLER_NAME = "EXITO";

   public ColombiaExitoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected List<String> getMainSellersNames() {
      return Arrays.asList(MAIN_SELLER_NAME);
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }
}
