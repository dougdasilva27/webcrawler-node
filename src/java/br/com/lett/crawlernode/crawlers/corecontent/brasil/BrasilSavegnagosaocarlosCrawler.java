package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SavegnagoCrawler;

public class BrasilSavegnagosaocarlosCrawler extends SavegnagoCrawler {
   public BrasilSavegnagosaocarlosCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getCEP() {
      return "13570640";
   }

   @Override
   protected String getCityCode() {
      return "5";
   }
}
