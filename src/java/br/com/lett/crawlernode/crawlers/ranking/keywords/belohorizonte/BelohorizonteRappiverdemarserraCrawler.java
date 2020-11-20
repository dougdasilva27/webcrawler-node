package br.com.lett.crawlernode.crawlers.ranking.keywords.belohorizonte;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.RappiCrawler;
import java.util.Arrays;

/**
 * Date: 19/10/20
 *
 * @author Fellype Layunne
 */
public class BelohorizonteRappiverdemarserraCrawler extends RappiCrawler {

   public BelohorizonteRappiverdemarserraCrawler(Session session) {
      super(session, Arrays.asList("900020342"));
   }

}
