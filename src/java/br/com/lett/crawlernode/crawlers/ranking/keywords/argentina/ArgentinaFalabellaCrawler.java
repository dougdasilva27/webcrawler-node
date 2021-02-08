package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.FalabellaCrawler;

public class ArgentinaFalabellaCrawler extends FalabellaCrawler {

   private static final String HOME_PAGE = "https://www.falabella.com.ar/falabella-ar/";

  public ArgentinaFalabellaCrawler(Session session) {
    super(session);
  }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
