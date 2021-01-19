package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.RappiCrawlerOld;

import java.util.Arrays;

public class BrasilRappigbarbosaaracajuCrawler extends RappiCrawlerOld {
   public BrasilRappigbarbosaaracajuCrawler(Session session) {
      super(session, Arrays.asList("900053930"));
   }
}

