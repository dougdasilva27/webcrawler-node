package br.com.lett.crawlernode.crawlers.corecontent.osasco;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.SupermercadonowCrawler;

public class OsascoSupermercadonowsupermercadoslopesjdcipavaCrawler extends SupermercadonowCrawler {


   public OsascoSupermercadonowsupermercadoslopesjdcipavaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercados-lopes-jd-cipava";
   }

   @Override
   protected String getSellerFullName() {
      return "Supermercados Lopes";
   }
}