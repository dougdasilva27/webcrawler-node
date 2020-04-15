package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.RappiCrawler;

public class SaopauloRappidrogaraiaCrawler extends RappiCrawler {

   public SaopauloRappidrogaraiaCrawler(Session session) {
      super(session, "raia", "lat=-23.584&lng=-46.671");
   }

}
