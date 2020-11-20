package br.com.lett.crawlernode.crawlers.corecontent.itaquaquecetuba;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SupermercadonowCrawler;

public class ItaquaquecetubaSupermercadonowsupermercadosdavoitaquaquecetubaCrawler extends SupermercadonowCrawler {


   public ItaquaquecetubaSupermercadonowsupermercadosdavoitaquaquecetubaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercado-d-avo-itaquaquecetuba";
   }

   @Override
   protected String getSellerFullName() {
      return "Supermercados D'avó";
   }
}