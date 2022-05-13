package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CommonMethods;
import org.json.JSONObject;

public class ClubeExtraKeywords extends LinxImpulseRanking {
   public ClubeExtraKeywords(Session session) {
      super(session);
   }

   @Override
   protected String crawlInternalId(JSONObject product, String internalPid) {
      String url = super.crawlProductUrl(product);
      if (url != null && !url.isEmpty()) {
         String internalId = CommonMethods.getLast(url.split("/"));
         if (internalId != null && !internalId.isEmpty()) {
            return internalId;
         }
      }
      return null;
   }
}
