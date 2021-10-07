package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.models.GPAKeywordsCrawler;
import org.json.JSONObject;

public class ExtraImplKeywords extends GPAKeywordsCrawler {

   public ExtraImplKeywords(Session session) {
      super(session);
      this.store = "deliveryextra";
      this.storeShort = "ex";
      this.homePageHttps = "https://www.clubeextra.com.br/";
   }

   @Override
   public String getStoreId() {
      return session.getOptions().optString("storeId");
   }

}
