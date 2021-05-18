package br.com.lett.crawlernode.crawlers.corecontent.barranquilla;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.HomecenterCrawler;

public class BarranquillaHomecenterCrawler extends HomecenterCrawler {

   public BarranquillaHomecenterCrawler(Session session) {
      super(session);
   }

   @Override
   public String getCity() {
      return "Barranquilla";
   }

   @Override
   public String getCityColuna() {
      return "4";
   }
}
