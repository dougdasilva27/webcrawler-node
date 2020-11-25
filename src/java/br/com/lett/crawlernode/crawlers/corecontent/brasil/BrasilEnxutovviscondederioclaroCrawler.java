package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.EnxutoSupermercadosCrawler;

public class BrasilEnxutovviscondederioclaroCrawler extends EnxutoSupermercadosCrawler {

   public static final String STORE_ID = "-8016551471851833557:6021852838134266709";

   public BrasilEnxutovviscondederioclaroCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}


