package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.SupermercadonowCrawler;

public class SaopauloSupermercadonowemporiohortisaboritaimCrawler extends SupermercadonowCrawler {


   public SaopauloSupermercadonowemporiohortisaboritaimCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "emporio-hortisabor-itaim";
   }

   @Override
   protected String getSellerFullName() {
      return "Empório Hortisabor";
   }
}