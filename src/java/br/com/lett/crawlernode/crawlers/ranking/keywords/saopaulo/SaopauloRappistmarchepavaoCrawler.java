package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.RappiCrawlerOld;
import java.util.Arrays;

/**
 * Date: 19/10/20
 *
 * @author Fellype Layunne
 */
public class SaopauloRappistmarchepavaoCrawler extends RappiCrawlerOld {

   public SaopauloRappistmarchepavaoCrawler(Session session) {
      super(session, Arrays.asList("900132997"));
   }

}
