package br.com.lett.crawlernode.crawlers.corecontent.colombia;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ColombiaRappiCrawler;
import org.json.JSONObject;

import java.util.List;

public class ColombiaRappiexitobogotaCrawler extends ColombiaRappiCrawler {
   
   public static final String STORE_ID = "6660081";

   public ColombiaRappiexitobogotaCrawler(Session session) {
      super(session);
      newUnification =true;
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }

   /*
   In the EQI audit, it detected that it does not have secondary images, although the api does, the images do not appear in the html.
    */
   @Override
   protected List<String> crawlSecondaryImages(JSONObject json, String primaryImage) {
      return null;
   }
}
