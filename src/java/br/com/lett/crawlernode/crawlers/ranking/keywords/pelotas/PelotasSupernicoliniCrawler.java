package br.com.lett.crawlernode.crawlers.ranking.keywords.pelotas;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.SupernicoliniCrawler;

public class PelotasSupernicoliniCrawler extends SupernicoliniCrawler {

   protected final String HOME_PAGE = "https://pelotas.supernicolini.com.br/";

   public PelotasSupernicoliniCrawler(Session session){ super(session); }

   @Override
   protected String getHomepage(){ return HOME_PAGE; }
}
