package br.com.lett.crawlernode.crawlers.ranking.keywords.juazeirodonorte;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SuperlagoaCrawler;

public class JuazeirodonorteSuperlagoajuazeirodonorteCrawler extends SuperlagoaCrawler {

   private static final String STORE_ID = "122";

   public JuazeirodonorteSuperlagoajuazeirodonorteCrawler(Session session) {
      super(session);
      super.setStoreId(STORE_ID);

   }

}
