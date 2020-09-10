package br.com.lett.crawlernode.crawlers.corecontent.pelotas;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.SupernicoliniCrawler;

public class PelotasSupernicoliniCrawler extends SupernicoliniCrawler {

   protected final String HOME_PAGE = "https://pelotas.supernicolini.com.br/";

   public PelotasSupernicoliniCrawler(Session session){ super(session); }

   @Override
   protected String getHomepage(){ return HOME_PAGE; }
}
