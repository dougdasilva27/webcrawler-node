package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.RappiCrawler;

import java.util.Collections;

public class SaopauloRappidrogasiljardimpaulistaCrawler extends RappiCrawler {
   public SaopauloRappidrogasiljardimpaulistaCrawler(Session session) {
      super(session, Collections.singletonList("900130114"));
   }
}
