package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.RappiCrawler;

import java.util.Arrays;

public class BrasilRappigbarbosaaracajuCrawler extends RappiCrawler {
   public BrasilRappigbarbosaaracajuCrawler(Session session) {
      super(session, Arrays.asList("900053930"));
   }
}

