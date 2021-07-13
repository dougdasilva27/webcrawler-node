package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SavegnagoCrawler;

public class BrasilSavegnagobarretosCrawler extends SavegnagoCrawler {
   public BrasilSavegnagobarretosCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getCityCode() {
      return "10";
   }
}
