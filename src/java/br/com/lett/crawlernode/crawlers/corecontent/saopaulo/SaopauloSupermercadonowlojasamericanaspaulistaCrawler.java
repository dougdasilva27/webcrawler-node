package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.SupermercadonowCrawler;

public class SaopauloSupermercadonowlojasamericanaspaulistaCrawler extends SupermercadonowCrawler {


   public SaopauloSupermercadonowlojasamericanaspaulistaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "pascoa-americanas-paulista";
   }

   @Override
   protected String getSellerFullName() {
      return "Lojas Americanas";
   }
}