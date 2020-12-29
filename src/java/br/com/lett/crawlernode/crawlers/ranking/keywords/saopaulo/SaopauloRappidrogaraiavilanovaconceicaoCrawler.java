package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.RappiCrawlerOld;
import java.util.Arrays;

public class SaopauloRappidrogaraiavilanovaconceicaoCrawler extends RappiCrawlerOld {

   public SaopauloRappidrogaraiavilanovaconceicaoCrawler(Session session) {
      super(session, Arrays.asList("900005057"));
   }
}
