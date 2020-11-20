package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.RappiCrawler;
import java.util.Collections;

public class SaopauloRappistmarcheitaimCrawler extends RappiCrawler {
   public SaopauloRappistmarcheitaimCrawler(Session session) {
      super(session, Collections.singletonList("900020365"));
   }
}
