package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.RappiCrawler;

import java.util.Collections;

public class SaopauloRappistmarchemoemaiiCrawler extends RappiCrawler {
   public SaopauloRappistmarchemoemaiiCrawler(Session session) {
      super(session, Collections.singletonList("900037057"));
   }
}
