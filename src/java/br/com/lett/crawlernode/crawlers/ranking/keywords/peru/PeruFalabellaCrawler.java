package br.com.lett.crawlernode.crawlers.ranking.keywords.peru;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.FalabellaCrawler;

public class PeruFalabellaCrawler extends FalabellaCrawler {

   private static final String HOME_PAGE = "https://www.falabella.com.pe/falabella-pe/";

   public PeruFalabellaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
