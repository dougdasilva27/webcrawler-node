package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.models.GPAKeywordsCrawler;
import org.json.JSONObject;

public class PaodeacucarKeywordsImpl extends GPAKeywordsCrawler {
   public PaodeacucarKeywordsImpl(Session session) {
      super(session);
      this.store = "paodeacucar";
      this.storeShort = "pa";
      this.homePageHttps = "https://www.paodeacucar.com/";
      this.cep = session.getOptions().getString("cep");
   }

   @Override
   public int getChooseStoreIdArrayPosition() {
      JSONObject options = session.getOptions();
      if (options.has("choose_store_position")) {
         return options.optInt("choose_store_position");
      } else {
         return 0;
      }
   }
}
