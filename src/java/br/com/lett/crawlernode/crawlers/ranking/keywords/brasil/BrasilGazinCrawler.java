package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.LinxImpulseRanking;
import org.json.JSONObject;

public class BrasilGazinCrawler extends LinxImpulseRanking {

   public BrasilGazinCrawler(Session session) {
      super(session);
   }

   @Override
   protected String crawlInternalId(JSONObject product, String internalPid) {
      return null;
   }
}
