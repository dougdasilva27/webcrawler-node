package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import java.util.Arrays;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.RappiCrawlerOld;

/**
 * Date: 19/10/20
 *
 * @author Fellype Layunne
 */
public class SaopauloRappicarrefourexpressCrawler extends RappiCrawlerOld {

   public SaopauloRappicarrefourexpressCrawler(Session session) {
      super(session, Arrays.asList("900132574"));
   }

}
