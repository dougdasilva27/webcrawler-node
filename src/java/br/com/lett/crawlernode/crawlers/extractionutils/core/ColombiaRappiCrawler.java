package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.session.Session;

public abstract class ColombiaRappiCrawler extends RappiCrawler {

   public ColombiaRappiCrawler(Session session) {
      super(session);
   }


   protected String getHomeDomain() {
      return "grability.rappi.com";
   }

   protected String getImagePrefix() {
      return "images.rappi.com/products";
   }

   @Override
   protected String getUrlPrefix() {
      return "producto";
   }

}
