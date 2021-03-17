package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SavegnagoCrawler;

public class BrasilSavegnagobebedouroCrawler extends SavegnagoCrawler {
   public BrasilSavegnagobebedouroCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getCEP() {
      return "14700500";
   }

   @Override
   protected String getCityCode() {
      return "9";
   }
}
