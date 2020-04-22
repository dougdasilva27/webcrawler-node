package br.com.lett.crawlernode.crawlers.corecontent.mogidascruzes;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.SupermercadonowCrawler;

public class MogidascruzesSupermercadonowsupermercadosdavomogidascruzesCrawler extends SupermercadonowCrawler {


   public MogidascruzesSupermercadonowsupermercadosdavomogidascruzesCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercado-d-avo-mogi-das-cruzes";
   }

   @Override
   protected String getSellerFullName() {
      return "Supermercados D'avó";
   }
}