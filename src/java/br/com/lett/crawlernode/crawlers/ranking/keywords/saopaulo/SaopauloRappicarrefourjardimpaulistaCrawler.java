package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.RappiCrawlerOld;
import java.util.Arrays;

/**
 * Date: 19/10/20
 *
 * @author Fellype Layunne
 */
public class SaopauloRappicarrefourjardimpaulistaCrawler extends RappiCrawlerOld {

   public SaopauloRappicarrefourjardimpaulistaCrawler(Session session) {
      super(session, Arrays.asList("900020401"));
   }

}
