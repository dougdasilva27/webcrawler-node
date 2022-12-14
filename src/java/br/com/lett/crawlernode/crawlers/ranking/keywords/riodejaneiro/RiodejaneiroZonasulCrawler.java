package br.com.lett.crawlernode.crawlers.ranking.keywords.riodejaneiro;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.VTEXGraphQLRanking;
import org.json.JSONObject;

public class RiodejaneiroZonasulCrawler extends VTEXGraphQLRanking {

   public RiodejaneiroZonasulCrawler(Session session) {
      super(session);
   }

   @Override
   protected String crawInternalPid(JSONObject product) {
      return null;
   }

}
