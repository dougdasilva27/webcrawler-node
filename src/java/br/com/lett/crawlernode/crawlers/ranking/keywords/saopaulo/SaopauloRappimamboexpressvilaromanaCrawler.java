package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.RappiCrawler;

import java.util.Arrays;

/**
 * Date: 19/10/20
 *
 * @author Fellype Layunne
 */
public class SaopauloRappimamboexpressvilaromanaCrawler extends RappiCrawler {

   public SaopauloRappimamboexpressvilaromanaCrawler(Session session) {
      super(session, Arrays.asList("900136785"));
   }

}
