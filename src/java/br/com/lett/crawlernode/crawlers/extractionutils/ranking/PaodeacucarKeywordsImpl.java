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
   }


}
