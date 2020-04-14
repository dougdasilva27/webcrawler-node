package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.RappiCrawler;

public class SaopauloRappimamboCrawler extends RappiCrawler {

   public SaopauloRappimamboCrawler(Session session) {
      super(session, "mambo", "lat=-23.584&lng=-46.671");
   }

}
