package br.com.lett.crawlernode.crawlers.ranking.keywords.riodejaneiro;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.RappiCrawler;

import java.util.Arrays;

public class RiodejaneiroRappizonasullaranjeirasCrawler extends RappiCrawler {

   public RiodejaneiroRappizonasullaranjeirasCrawler(Session session) {
      super(session, Arrays.asList("900141059"));
   }

}
