package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.RappiCrawler;

import java.util.Arrays;
import java.util.List;

public class SaopauloRappidrogaraiasaogabrielCrawler extends RappiCrawler {

   public SaopauloRappidrogaraiasaogabrielCrawler(Session session) {
      super(session, Arrays.asList("900130103"));
   }
}
