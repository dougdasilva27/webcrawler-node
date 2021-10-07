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
   }

}
