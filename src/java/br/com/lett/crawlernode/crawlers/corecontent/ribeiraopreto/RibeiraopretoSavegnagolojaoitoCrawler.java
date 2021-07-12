package br.com.lett.crawlernode.crawlers.corecontent.ribeiraopreto;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SavegnagoCrawler;

public class RibeiraopretoSavegnagolojaoitoCrawler extends SavegnagoCrawler {

   public RibeiraopretoSavegnagolojaoitoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getCityCode() {
      return "2";
   }


}
