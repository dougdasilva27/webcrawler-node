package br.com.lett.crawlernode.crawlers.corecontent.cali;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.HomecenterCrawler;

public class CaliHomecenterCrawler extends HomecenterCrawler {

   public CaliHomecenterCrawler(Session session) {
      super(session);
   }

   @Override
   public String getCity() {
      return "Cali";
   }

   @Override
   public String getCityColuna() {
      return "3";
   }

}
