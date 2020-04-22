package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.SupermercadonowCrawler;

public class SaopauloSupermercadonowlojasamericanasvilamarianaCrawler extends SupermercadonowCrawler {


   public SaopauloSupermercadonowlojasamericanasvilamarianaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "pascoa-americanas-vila-mariana";
   }

   @Override
   protected String getSellerFullName() {
      return "Lojas Americanas";
   }
}