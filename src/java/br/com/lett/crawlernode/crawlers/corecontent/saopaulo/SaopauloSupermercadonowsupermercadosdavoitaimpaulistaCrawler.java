package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SupermercadonowCrawler;

public class SaopauloSupermercadonowsupermercadosdavoitaimpaulistaCrawler extends SupermercadonowCrawler {


   public SaopauloSupermercadonowsupermercadosdavoitaimpaulistaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercado-d-avo-itaim-paulista";
   }

   @Override
   protected String getSellerFullName() {
      return "Supermercados D'avó";
   }
}