package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import java.util.Arrays;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.RappiCrawlerOld;

public class SaopauloRappistmarcheportaldomorumbiCrawler extends RappiCrawlerOld {

   public SaopauloRappistmarcheportaldomorumbiCrawler(Session session) {
      super(session, Arrays.asList("900033773"));
   }

}
