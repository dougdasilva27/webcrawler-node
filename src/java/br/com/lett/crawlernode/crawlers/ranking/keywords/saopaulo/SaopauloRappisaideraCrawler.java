package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import java.util.Arrays;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.RappiCrawler;

public class SaopauloRappisaideraCrawler extends RappiCrawler {

   public SaopauloRappisaideraCrawler(Session session) {
      super(session, Arrays.asList("900006685"));
   }

}