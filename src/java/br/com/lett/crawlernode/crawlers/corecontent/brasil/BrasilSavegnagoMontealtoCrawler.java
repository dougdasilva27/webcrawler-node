package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SavegnagoCrawler;

public class BrasilSavegnagoMontealtoCrawler extends SavegnagoCrawler {

   public BrasilSavegnagoMontealtoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getCEP() {
      return "15910000";
   }

   @Override
   protected String getCityCode() {
      return "12";
   }

}
