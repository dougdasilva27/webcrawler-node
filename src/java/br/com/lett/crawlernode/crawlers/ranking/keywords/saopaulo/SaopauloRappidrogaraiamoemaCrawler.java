package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.RappiCrawlerOld;
import java.util.Arrays;

public class SaopauloRappidrogaraiamoemaCrawler extends RappiCrawlerOld {

   public SaopauloRappidrogaraiamoemaCrawler(Session session) {
      super(session, Arrays.asList("900004068"));
   }

}
