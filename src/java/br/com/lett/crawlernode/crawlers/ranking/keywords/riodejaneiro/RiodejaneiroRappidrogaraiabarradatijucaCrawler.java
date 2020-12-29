package br.com.lett.crawlernode.crawlers.ranking.keywords.riodejaneiro;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.RappiCrawlerOld;
import java.util.Arrays;

public class RiodejaneiroRappidrogaraiabarradatijucaCrawler extends RappiCrawlerOld {

   public RiodejaneiroRappidrogaraiabarradatijucaCrawler(Session session) {
      super(session, Arrays.asList("900006787"));
   }

}
