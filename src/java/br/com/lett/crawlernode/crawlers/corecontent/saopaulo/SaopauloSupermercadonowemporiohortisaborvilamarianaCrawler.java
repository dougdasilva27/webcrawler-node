package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SupermercadonowCrawler;

public class SaopauloSupermercadonowemporiohortisaborvilamarianaCrawler extends SupermercadonowCrawler {


   public SaopauloSupermercadonowemporiohortisaborvilamarianaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "emporio-hortisabor-vl-mariana";
   }

   @Override
   protected String getSellerFullName() {
      return "Empório Hortisabor";
   }
}