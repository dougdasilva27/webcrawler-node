package br.com.lett.crawlernode.crawlers.corecontent.peru;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.TottusCrawler;


public class PeruTottusCrawler extends TottusCrawler {

   private static final String home_page = "https://www.tottus.com.pe/";
   private static final String seller_full_name = "Tottus-peru";
   
   public PeruTottusCrawler(Session session) {
      super(session);
      super.homePage = home_page;
      super.salerName = seller_full_name;
   }
}

