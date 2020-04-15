package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.SupermercadonowCrawler;

public class SaopauloSupermercadonowsupermercadosdavooratorioCrawler extends SupermercadonowCrawler {


   public SaopauloSupermercadonowsupermercadosdavooratorioCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercado-d-avo-av-do-oratorio";
   }

   @Override
   protected String getSellerFullName() {
      return "Supermercados D'avó";
   }
}