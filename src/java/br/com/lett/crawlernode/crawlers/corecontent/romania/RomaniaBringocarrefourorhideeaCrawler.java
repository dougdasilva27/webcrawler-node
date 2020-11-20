package br.com.lett.crawlernode.crawlers.corecontent.romania;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.RomaniaBringoCrawler;

public class RomaniaBringocarrefourorhideeaCrawler extends RomaniaBringoCrawler {

   private static final String SELLER_FULL_NAME = "Carrefour Orhideea";

   public RomaniaBringocarrefourorhideeaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getMainSeller() {
      return SELLER_FULL_NAME;
   }
}
