package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import java.util.Arrays;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.RappiCrawler;

public class SaopauloRappistmarcheCrawler extends RappiCrawler {

   public SaopauloRappistmarcheCrawler(Session session) {
      super(session, Arrays.asList("900020814"));
   }

}
