package br.com.lett.crawlernode.crawlers.ranking.keywords.riodejaneiro;

import java.util.Arrays;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.RappiCrawlerOld;

public class RiodejaneiroRappimundialriachueloCrawler extends RappiCrawlerOld {

   public RiodejaneiroRappimundialriachueloCrawler(Session session) {
      super(session, Arrays.asList("900127219"));
   }

}
