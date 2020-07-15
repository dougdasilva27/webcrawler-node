package br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.SupermercadonowCrawler;

public class RiodejaneiroSupermercadonowhortifrutiflamengoCrawler extends SupermercadonowCrawler {


   public RiodejaneiroSupermercadonowhortifrutiflamengoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "hortifruti-flamengo";
   }

   @Override
   protected String getSellerFullName() {
      return "Hortifruti";
   }
}