package br.com.lett.crawlernode.crawlers.corecontent.suzano;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SupermercadonowCrawler;

public class SuzanoSupermercadonowsupermercadosdavosuzanoCrawler extends SupermercadonowCrawler {


   public SuzanoSupermercadonowsupermercadosdavosuzanoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercado-d-avo-suzano";
   }

   @Override
   protected String getSellerFullName() {
      return "Supermercados D'avó";
   }
}