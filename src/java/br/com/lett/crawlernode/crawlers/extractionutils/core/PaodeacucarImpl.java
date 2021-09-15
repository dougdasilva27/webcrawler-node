package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.session.Session;
import org.json.JSONObject;

public class PaodeacucarImpl extends GPACrawler {

   public PaodeacucarImpl(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.FETCHER);
      this.store = "pa";
      this.homePageHttps = "https://www.paodeacucar.com/";
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
