package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SupermercadonowCrawler;

public class SaopauloSupermercadonowlojanestlechucrizaidanCrawler extends SupermercadonowCrawler {


   public SaopauloSupermercadonowlojanestlechucrizaidanCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "loja-nestle-chucri-zaidan";
   }

   @Override
   protected String getSellerFullName() {
      return "Empório Nestle";
   }
}
