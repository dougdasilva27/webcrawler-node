package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SavegnagoCrawler;

public class BrasilSavegnagoRioclaroCrawler extends SavegnagoCrawler {

   public BrasilSavegnagoRioclaroCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getCEP() {
      return "13500130";
   }

   @Override
   protected String getCityCode() {
      return "13";
   }

}
