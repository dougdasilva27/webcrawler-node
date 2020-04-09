package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.SupermercadonowCrawler;

public class SaopauloSupermercadonowmastersupermercadosconsolacaoCrawler extends SupermercadonowCrawler {


   public SaopauloSupermercadonowmastersupermercadosconsolacaoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "master-supermercados-consolacao";
   }

   @Override
   protected String getSellerFullName() {
      return "Master Supermercados";
   }
}