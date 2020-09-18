package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import java.util.Arrays;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.RappiCrawler;

public class SaopauloRappiatacadaoCrawler extends RappiCrawler {

   public SaopauloRappiatacadaoCrawler(Session session) {
      super(session, Arrays.asList("900136949"));
   }

}
