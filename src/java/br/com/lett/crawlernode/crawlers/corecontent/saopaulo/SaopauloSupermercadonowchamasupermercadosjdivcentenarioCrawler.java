package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.SupermercadonowCrawler;

public class SaopauloSupermercadonowchamasupermercadosjdivcentenarioCrawler extends SupermercadonowCrawler {


   public SaopauloSupermercadonowchamasupermercadosjdivcentenarioCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "chama-supermercados-jd-iv-centenario";
   }

   @Override
   protected String getSellerFullName() {
      return "Chama Supermercados";
   }
}
