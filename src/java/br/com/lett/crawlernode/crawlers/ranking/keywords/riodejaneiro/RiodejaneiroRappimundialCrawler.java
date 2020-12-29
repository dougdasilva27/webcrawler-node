package br.com.lett.crawlernode.crawlers.ranking.keywords.riodejaneiro;

import java.util.Arrays;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.RappiCrawlerOld;

public class RiodejaneiroRappimundialCrawler extends RappiCrawlerOld {

   public RiodejaneiroRappimundialCrawler(Session session) {
      super(session, Arrays.asList("900020828"));
   }

}
