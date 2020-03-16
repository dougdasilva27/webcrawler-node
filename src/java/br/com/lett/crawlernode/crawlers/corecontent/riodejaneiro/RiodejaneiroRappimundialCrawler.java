package br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.RappiCrawler;

public class RiodejaneiroRappimundialCrawler extends RappiCrawler {

   public RiodejaneiroRappimundialCrawler(Session session) {
      super(session, "mundial", "lat=-22.952&lng=-43.192");
   }

}
