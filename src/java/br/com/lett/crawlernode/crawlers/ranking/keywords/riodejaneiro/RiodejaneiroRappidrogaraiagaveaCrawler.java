package br.com.lett.crawlernode.crawlers.ranking.keywords.riodejaneiro;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.RappiCrawlerOld;
import java.util.Arrays;

public class RiodejaneiroRappidrogaraiagaveaCrawler extends RappiCrawlerOld {

   public RiodejaneiroRappidrogaraiagaveaCrawler(Session session) {
      super(session, Arrays.asList("900005261"));
   }

}
