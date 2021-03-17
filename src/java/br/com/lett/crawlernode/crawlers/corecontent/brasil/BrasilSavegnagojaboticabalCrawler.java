package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SavegnagoCrawler;

public class BrasilSavegnagojaboticabalCrawler extends SavegnagoCrawler {

   public BrasilSavegnagojaboticabalCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getCEP() {
      return "14870370";
   }

   @Override
   protected String getCityCode() {
      return "7";
   }

}
