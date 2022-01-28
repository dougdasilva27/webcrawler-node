package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.LinxImpulseRanking;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class BrasilHavanCrawler extends LinxImpulseRanking {

   public BrasilHavanCrawler(Session session) {
      super(session);
   }

   @Override
   protected String crawlInternalPid(JSONObject product) {
      Object pid = product.optQuery("/skus/0/properties/details/idAddToCart");
      if(pid != null) {
         return pid.toString();
      }
      return super.crawlInternalPid(product);
   }
}
