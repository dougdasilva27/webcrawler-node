package br.com.lett.crawlernode.crawlers.corecontent.pelotas;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SupernicoliniCrawler;

public class PelotasSupernicoliniCrawler extends SupernicoliniCrawler {

   protected static final String HOME_PAGE = "https://pelotas.supernicolini.com.br/";

   public PelotasSupernicoliniCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomepage() {
      return HOME_PAGE;
   }
}
