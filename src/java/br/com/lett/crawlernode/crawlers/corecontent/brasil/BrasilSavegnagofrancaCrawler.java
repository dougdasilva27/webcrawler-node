package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SavegnagoCrawler;

public class BrasilSavegnagofrancaCrawler extends SavegnagoCrawler {

   public BrasilSavegnagofrancaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getCEP() {
      return "14406730";
   }

   @Override
   protected String getCityCode() {
      return "3";
   }

}
