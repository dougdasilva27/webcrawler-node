package br.com.lett.crawlernode.crawlers.corecontent.piracicaba;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.EnxutoSupermercadosCrawler;

public class PiracicabaEnxutocassiopaschoalCrawler extends EnxutoSupermercadosCrawler {

   public static final String STORE_ID = "-688185621084245752:-7896405111134805819";

   public PiracicabaEnxutocassiopaschoalCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}


