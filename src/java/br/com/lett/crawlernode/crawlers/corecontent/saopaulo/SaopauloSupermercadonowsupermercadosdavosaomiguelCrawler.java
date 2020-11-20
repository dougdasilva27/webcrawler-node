package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SupermercadonowCrawler;

public class SaopauloSupermercadonowsupermercadosdavosaomiguelCrawler extends SupermercadonowCrawler {


   public SaopauloSupermercadonowsupermercadosdavosaomiguelCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercado-d-avo-sao-miguel";
   }

   @Override
   protected String getSellerFullName() {
      return "Supermercados D'avó";
   }
}