package br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.SupermercadonowCrawler;

public class RiodejaneiroSupermercadonowlojasamericanasipanemaCrawler extends SupermercadonowCrawler {


   public RiodejaneiroSupermercadonowlojasamericanasipanemaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "pascoa-americanas-ipanema";
   }

   @Override
   protected String getSellerFullName() {
      return "Lojas Americanas";
   }
}