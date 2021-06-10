package br.com.lett.crawlernode.crawlers.ranking.keywords.colombia;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.VtexRankingKeywordsNew;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ColombiaTiendahogaruniversalCrawler extends VtexRankingKeywordsNew {

   @Override
   protected String setHomePage() {
      return "https://www.tiendahogaruniversal.com";
   }

   public ColombiaTiendahogaruniversalCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

}
