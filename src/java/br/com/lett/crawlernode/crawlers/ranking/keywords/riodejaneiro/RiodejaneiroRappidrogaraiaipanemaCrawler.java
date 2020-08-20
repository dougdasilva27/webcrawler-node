package br.com.lett.crawlernode.crawlers.ranking.keywords.riodejaneiro;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.RappiCrawler;

import java.util.Arrays;

public class RiodejaneiroRappidrogaraiaipanemaCrawler extends RappiCrawler {

   public RiodejaneiroRappidrogaraiaipanemaCrawler(Session session) {
      super(session, Arrays.asList("900005268"));
   }

}
