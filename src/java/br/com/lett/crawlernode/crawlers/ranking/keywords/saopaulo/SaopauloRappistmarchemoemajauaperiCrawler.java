package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import java.util.Arrays;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.RappiCrawlerOld;

public class SaopauloRappistmarchemoemajauaperiCrawler extends RappiCrawlerOld {

   public SaopauloRappistmarchemoemajauaperiCrawler(Session session) {
      super(session, Arrays.asList("900037057"));
   }

}
