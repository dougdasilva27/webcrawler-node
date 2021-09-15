package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.models.GPAKeywordsCrawler;
import org.json.JSONObject;

public class ExtraImplKeywords extends GPAKeywordsCrawler {

   public ExtraImplKeywords(Session session) {
      super(session);
      this.store = "extra";
      this.storeShort = "ex";
      this.homePageHttps = "https://www.clubeextra.com.br/";
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
