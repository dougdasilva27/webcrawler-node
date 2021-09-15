package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.session.Session;
import org.json.JSONObject;

public class ExtraImpl extends GPACrawler {

   public ExtraImpl(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.FETCHER);
      this.store = "ex";
      this.homePageHttps = "https://www.clubeextra.com.br/";
      this.cep = session.getOptions().getString("cep");
   }

   @Override
   public String getChooseStoreIdArrayPosition() {
      JSONObject options = session.getOptions();
      if (options.has("choose_store_position")) {
         return options.optString("choose_store_position");
      } else {
         return "0";
      }
   }
}
