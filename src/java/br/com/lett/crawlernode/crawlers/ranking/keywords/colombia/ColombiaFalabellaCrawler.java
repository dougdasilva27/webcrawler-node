package br.com.lett.crawlernode.crawlers.ranking.keywords.colombia;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.FalabellaCrawler;

public class ColombiaFalabellaCrawler extends FalabellaCrawler {

   private static final String HOME_PAGE = "https://www.falabella.com.co/falabella-co/";

   public ColombiaFalabellaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

}
