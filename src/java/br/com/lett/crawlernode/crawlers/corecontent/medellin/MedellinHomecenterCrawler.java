package br.com.lett.crawlernode.crawlers.corecontent.medellin;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.HomecenterCrawler;

public class MedellinHomecenterCrawler extends HomecenterCrawler {

   public MedellinHomecenterCrawler(Session session) {
      super(session);
   }

   @Override
   public String getCity() {
      return "Medellin";
   }

   @Override
   public String getCityComuna() {
      return "2";
   }
}
