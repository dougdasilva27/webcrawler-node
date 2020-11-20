package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SupermercadonowCrawler;

public class SaopauloSupermercadonowlojasamericanaslapaCrawler extends SupermercadonowCrawler {


   public SaopauloSupermercadonowlojasamericanaslapaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "pascoa-americanas-lapa";
   }

   @Override
   protected String getSellerFullName() {
      return "Lojas Americanas";
   }
}