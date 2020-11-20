package br.com.lett.crawlernode.crawlers.corecontent.bage;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SupernicoliniCrawler;

public class BageSupernicoliniCrawler extends SupernicoliniCrawler {

   protected final String HOME_PAGE = "https://bage.supernicolini.com.br/";

   public BageSupernicoliniCrawler(Session session){ super(session); }

   @Override
   protected String getHomepage(){ return HOME_PAGE; }

}
