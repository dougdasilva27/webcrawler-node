package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SupermercadonowCrawler;

public class SaopauloSupermercadonowsupermercadosdavoitaqueraCrawler extends SupermercadonowCrawler {


   public SaopauloSupermercadonowsupermercadosdavoitaqueraCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercado-d-avo-itaquera";
   }

   @Override
   protected String getSellerFullName() {
      return "Supermercados D'avó";
   }
}