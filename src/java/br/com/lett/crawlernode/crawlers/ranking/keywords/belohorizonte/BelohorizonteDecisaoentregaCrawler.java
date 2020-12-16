package br.com.lett.crawlernode.crawlers.ranking.keywords.belohorizonte;

import java.util.HashMap;
import java.util.Map;

import br.com.lett.crawlernode.crawlers.extractionutils.ranking.VipcommerceRanking;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;

public class BelohorizonteDecisaoentregaCrawler extends VipcommerceRanking {
   private static final String DOMAIN = "decisaoentrega.com.br";

   public BelohorizonteDecisaoentregaCrawler(Session session) {
      super(session);
   }

   @Override
   public String getDomain() {
      return DOMAIN;
   }
}
