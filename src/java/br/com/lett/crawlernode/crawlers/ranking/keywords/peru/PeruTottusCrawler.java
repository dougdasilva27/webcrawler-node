package br.com.lett.crawlernode.crawlers.ranking.keywords.peru;

import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.TottusCrawler;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class PeruTottusCrawler extends TottusCrawler {

   private static final String home_page = "www.tottus.com.pe";

   public PeruTottusCrawler(Session session) {
      super(session);
      super.homePage = home_page;
   }
}
