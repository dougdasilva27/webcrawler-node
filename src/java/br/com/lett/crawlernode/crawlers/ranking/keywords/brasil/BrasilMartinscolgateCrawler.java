package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.MartinsKeywords;

public class BrasilMartinscolgateCrawler extends MartinsKeywords {

  public BrasilMartinscolgateCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getPassword() {
      return "roegelin83";
   }

   @Override
   protected String getLogin() {
      return "f_roegelin@hotmail.com";
   }
}
