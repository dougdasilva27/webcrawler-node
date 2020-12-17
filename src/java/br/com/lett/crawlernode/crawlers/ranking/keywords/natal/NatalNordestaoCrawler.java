package br.com.lett.crawlernode.crawlers.ranking.keywords.natal;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;

import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.VipcommerceRanking;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class NatalNordestaoCrawler extends VipcommerceRanking {
   private static final String DOMAIN = "cliqueretire.nordestao.com.br";

   public NatalNordestaoCrawler(Session session) {
      super(session);
   }

   @Override
   public String getDomain() {
      return DOMAIN;
   }
}
