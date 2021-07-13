package br.com.lett.crawlernode.crawlers.corecontent.ribeiraopreto;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SavegnagoCrawler;

public class RibeiraopretoSavegnagoCrawler extends SavegnagoCrawler {

   public RibeiraopretoSavegnagoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getCityCode() {
      return "1";
   }


}
