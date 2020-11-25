package br.com.lett.crawlernode.crawlers.corecontent.campinas;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.EnxutoSupermercadosCrawler;

public class CampinasEnxutopoemaCrawler extends EnxutoSupermercadosCrawler {

   public static final String STORE_ID = "4216966415564441288:3329867827654294844";

   public CampinasEnxutopoemaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}

