package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import java.util.Arrays;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.RappiCrawler;

public class SaopauloRappistmarcheportaldomorumbiCrawler extends RappiCrawler {

   public SaopauloRappistmarcheportaldomorumbiCrawler(Session session) {
      super(session, Arrays.asList("900033773"));
   }

}
