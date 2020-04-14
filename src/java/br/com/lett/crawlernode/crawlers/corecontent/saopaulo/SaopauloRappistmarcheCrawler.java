package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.RappiCrawler;

public class SaopauloRappistmarcheCrawler extends RappiCrawler {

   public SaopauloRappistmarcheCrawler(Session session) {
      super(session, "st-marche", "lat=-23.584&lng=-46.671");
   }

}
