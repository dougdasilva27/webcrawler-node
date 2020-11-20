package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SupermercadonowCrawler;

public class SaopauloSupermercadonowchamasupermercadosregentefeijoCrawler extends SupermercadonowCrawler {


   public SaopauloSupermercadonowchamasupermercadosregentefeijoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "chama-supermercados-regente-feijo";
   }

   @Override
   protected String getSellerFullName() {
      return "Chama Supermercados";
   }
}
