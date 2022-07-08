package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.FalabellaCrawler;

public class ChileFalabellaCrawler extends FalabellaCrawler {

   public ChileFalabellaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return session.getOptions().optString("home_page");
   }
   @Override
   protected boolean isAllow3pSeller() {
      return session.getOptions().optBoolean("allow_3p_seller", true);
   }
}
