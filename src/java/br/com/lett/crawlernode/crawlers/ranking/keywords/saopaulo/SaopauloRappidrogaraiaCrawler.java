package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import java.util.Arrays;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.RappiCrawlerOld;

public class SaopauloRappidrogaraiaCrawler extends RappiCrawlerOld {

   public SaopauloRappidrogaraiaCrawler(Session session) {
      super(session, Arrays.asList("900004148"));
   }

}
