package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.TottusCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class ChileTottusCrawler extends TottusCrawler {

   private static final String home_page = "www.tottus.cl";

   public ChileTottusCrawler(Session session) {
      super(session);
      super.homePage = home_page;
   }
}
