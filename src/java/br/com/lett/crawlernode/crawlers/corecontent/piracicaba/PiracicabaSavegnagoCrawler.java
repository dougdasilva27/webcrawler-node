package br.com.lett.crawlernode.crawlers.corecontent.piracicaba;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SavegnagoCrawler;

public class PiracicabaSavegnagoCrawler extends SavegnagoCrawler {

   public PiracicabaSavegnagoCrawler(Session session) {
      super(session);
   }

   protected String getCityCode() {
      return "14";
   }
}
