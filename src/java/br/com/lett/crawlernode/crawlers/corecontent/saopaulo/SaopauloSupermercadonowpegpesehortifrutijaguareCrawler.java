package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SupermercadonowCrawler;

public class SaopauloSupermercadonowpegpesehortifrutijaguareCrawler extends SupermercadonowCrawler {


   public SaopauloSupermercadonowpegpesehortifrutijaguareCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "peg-pese-jaguare";
   }

   @Override
   protected String getSellerFullName() {
      return "Peg Pese Hortifruti";
   }
}