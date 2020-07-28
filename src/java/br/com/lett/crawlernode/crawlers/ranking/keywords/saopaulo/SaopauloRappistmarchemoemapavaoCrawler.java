package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import java.util.Arrays;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.RappiCrawler;

public class SaopauloRappistmarchemoemapavaoCrawler extends RappiCrawler {

   public SaopauloRappistmarchemoemapavaoCrawler(Session session) {
      super(session, Arrays.asList("900037057"));
   }

}
