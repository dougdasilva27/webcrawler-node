package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.FalabellaCrawler;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.TottusCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class ChileTottusCrawler extends FalabellaCrawler {

   public ChileTottusCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return session.getOptions().optString("home_page");
   }
   @Override
   protected boolean isAllow3pSeller() {
      return session.getOptions().optBoolean("allow_3p_seller", true);
   }

}
