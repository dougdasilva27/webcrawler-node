package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SavegnagoCrawler;

public class BrasilSavegnagomataoCrawler extends SavegnagoCrawler {


   public BrasilSavegnagomataoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getCEP() {
      return "15990005";
   }

   @Override
   protected String getCityCode() {
      return "8";
   }
}
