package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SavegnagoCrawler;

public class BrasilSavegnagoararaquaraCrawler extends SavegnagoCrawler {
   public BrasilSavegnagoararaquaraCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getCEP() {
      return "14801260";
   }

   @Override
   protected String getCityCode() {
      return "4";
   }
}
