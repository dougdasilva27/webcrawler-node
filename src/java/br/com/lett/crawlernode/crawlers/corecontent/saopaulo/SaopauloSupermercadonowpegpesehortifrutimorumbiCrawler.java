package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SupermercadonowCrawler;

public class SaopauloSupermercadonowpegpesehortifrutimorumbiCrawler extends SupermercadonowCrawler {


   public SaopauloSupermercadonowpegpesehortifrutimorumbiCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "peg-pese-morumbi";
   }

   @Override
   protected String getSellerFullName() {
      return "Peg Pese Hortifruti";
   }
}