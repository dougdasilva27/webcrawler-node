package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.FalabellaCrawler;

public class ChileFalabellaCrawler extends FalabellaCrawler {

   private static final String HOME_PAGE = "https://www.falabella.com/falabella-cl/";

   public ChileFalabellaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
