package br.com.lett.crawlernode.crawlers.ranking.keywords.bage;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.SupernicoliniCrawler;

public class BageSupernicoliniCrawler extends SupernicoliniCrawler {

   protected final String HOME_PAGE = "https://bage.supernicolini.com.br/";

   public BageSupernicoliniCrawler(Session session){ super(session); }

   @Override
   protected String getHomepage(){ return HOME_PAGE; }

}
