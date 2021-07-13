package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SavegnagoCrawler;

public class BrasilSavegnagomontealtoCrawler extends SavegnagoCrawler {

   public BrasilSavegnagomontealtoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getCityCode() {
      return "12";
   }

}
