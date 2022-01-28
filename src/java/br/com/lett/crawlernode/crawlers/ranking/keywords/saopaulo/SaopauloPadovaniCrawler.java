package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.LinxImpulseRanking;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SaopauloPadovaniCrawler extends LinxImpulseRanking {
   private String internalId = "";

   public SaopauloPadovaniCrawler(Session session) {
      super(session);
   }

   @Override
   protected String crawlInternalPid(JSONObject product) {
      String internalPid = super.crawlInternalPid(product);
      this.internalId = super.crawlInternalId(product, internalPid);

      return internalId;
   }

   @Override
   protected String crawlInternalId(JSONObject product, String internalPid) {
      return this.internalId;
   }
}
