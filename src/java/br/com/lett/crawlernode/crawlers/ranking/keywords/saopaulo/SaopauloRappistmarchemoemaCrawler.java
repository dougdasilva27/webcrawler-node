package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.RappiCrawlerOld;
import java.util.Collections;

public class SaopauloRappistmarchemoemaCrawler extends RappiCrawlerOld {
   public SaopauloRappistmarchemoemaCrawler(Session session) {
      super(session, Collections.singletonList("900037057"));
   }
}
