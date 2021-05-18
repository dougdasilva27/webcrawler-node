package br.com.lett.crawlernode.crawlers.corecontent.colombia;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.HomecenterCrawler;

public class ColombiaHomecenterCrawler extends HomecenterCrawler {

   public ColombiaHomecenterCrawler(Session session) {
      super(session);
   }

   @Override
   public String getCity() {
      return null;
   }

   @Override
   public String getCityComuna() {
      return null;
   }
}
