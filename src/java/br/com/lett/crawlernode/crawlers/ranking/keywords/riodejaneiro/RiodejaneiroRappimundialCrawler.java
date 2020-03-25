package br.com.lett.crawlernode.crawlers.ranking.keywords.riodejaneiro;

import java.util.Arrays;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.RappiCrawler;

public class RiodejaneiroRappimundialCrawler extends RappiCrawler {

   public RiodejaneiroRappimundialCrawler(Session session) {
      super(session, Arrays.asList("900020828"));
   }

}
