package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import java.util.Arrays;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.RappiCrawler;

/**
 * Date: 19/10/20
 *
 * @author Fellype Layunne
 */
public class SaopauloRappicarrefourexpressCrawler extends RappiCrawler {

   public SaopauloRappicarrefourexpressCrawler(Session session) {
      super(session, Arrays.asList("900132574"));
   }

}
