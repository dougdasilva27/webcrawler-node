package br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SupermercadonowCrawler;

public class RiodejaneiroSupermercadonowlojasamericanaslaranjeirasCrawler extends SupermercadonowCrawler {


   public RiodejaneiroSupermercadonowlojasamericanaslaranjeirasCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "pascoa-americanas-laranjeiras";
   }

   @Override
   protected String getSellerFullName() {
      return "Lojas Americanas";
   }
}