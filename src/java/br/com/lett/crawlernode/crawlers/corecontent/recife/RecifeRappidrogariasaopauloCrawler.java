package br.com.lett.crawlernode.crawlers.corecontent.recife;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilRappiCrawler;

public class RecifeRappidrogariasaopauloCrawler extends BrasilRappiCrawler {
   public RecifeRappidrogariasaopauloCrawler(Session session) {
      super(session);
      newUnification = true;
   }

   @Override
   protected String getStoreId() {
      return "900153585";
   }
}
