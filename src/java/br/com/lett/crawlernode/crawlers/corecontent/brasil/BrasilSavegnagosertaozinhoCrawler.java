package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SavegnagoCrawler;

public class BrasilSavegnagosertaozinhoCrawler extends SavegnagoCrawler {
   public BrasilSavegnagosertaozinhoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getCEP() {
      return "14170000";
   }

   @Override
   protected String getCityCode() {
      return "6";
   }
}