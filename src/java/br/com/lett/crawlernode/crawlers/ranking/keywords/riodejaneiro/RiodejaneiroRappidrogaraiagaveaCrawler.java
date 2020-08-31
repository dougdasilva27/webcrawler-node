package br.com.lett.crawlernode.crawlers.ranking.keywords.riodejaneiro;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.RappiCrawler;

import java.util.Arrays;

public class RiodejaneiroRappidrogaraiagaveaCrawler extends RappiCrawler {

   public RiodejaneiroRappidrogaraiagaveaCrawler(Session session) {
      super(session, Arrays.asList("900005261"));
   }

}
