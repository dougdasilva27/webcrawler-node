package br.com.lett.crawlernode.crawlers.corecontent.juazeirodonorte;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SuperlagoaCrawler;

public class JuazeirodonorteSuperlagoajuazeirodonorteCrawler extends SuperlagoaCrawler {

   private static final String STORE_ID = "122";

   public JuazeirodonorteSuperlagoajuazeirodonorteCrawler(Session session) {
      super(session);
      super.setStoreId(STORE_ID);

   }

}
