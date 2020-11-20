package br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SupermercadonowCrawler;

public class RiodejaneiroSupermercadonowlojasamericanasbarradatijucaCrawler extends SupermercadonowCrawler {


   public RiodejaneiroSupermercadonowlojasamericanasbarradatijucaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "pascoa-americanas-barra-da-tijuca";
   }

   @Override
   protected String getSellerFullName() {
      return "Lojas Americanas";
   }
}