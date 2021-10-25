package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.session.Session;
import org.json.JSONObject;

public class PaodeacucarImpl extends GPACrawler {

   public PaodeacucarImpl(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.JSOUP);
      this.store = "pa";
      this.homePageHttps = "https://www.paodeacucar.com/";
   }


}
