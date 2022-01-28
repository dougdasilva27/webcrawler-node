package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.crawlers.extractionutils.ranking.LinxImpulseRanking;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

import java.io.UnsupportedEncodingException;

public class BrasilDrogalCrawler extends LinxImpulseRanking {
   protected String internalPid = "";
   protected String internalId = "";

   public BrasilDrogalCrawler(Session session) {
      super(session);
   }

   // Inverts internalPid and internalId
   @Override
   protected String crawlInternalPid(JSONObject product) {
      this.internalPid = super.crawlInternalPid(product);
      this.internalId = super.crawlInternalId(product, this.internalPid);

      return internalId;
   }

   @Override
   protected String crawlInternalId(JSONObject product, String internalPid) {
      return this.internalPid;
   }
}
