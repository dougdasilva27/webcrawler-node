package br.com.lett.crawlernode.crawlers.ranking.keywords.florianopolis;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.RappiCrawlerOld;
import java.util.Arrays;

/**
 * Date: 19/10/20
 *
 * @author Fellype Layunne
 */
public class FlorianopolisRappiangelonijardimatlanticoCrawler extends RappiCrawlerOld {

   public FlorianopolisRappiangelonijardimatlanticoCrawler(Session session) {
      super(session, Arrays.asList("900049319"));
   }

}

