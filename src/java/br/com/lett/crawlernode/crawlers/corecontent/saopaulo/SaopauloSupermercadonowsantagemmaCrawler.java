package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SupermercadonowCrawler;

public class SaopauloSupermercadonowsantagemmaCrawler extends SupermercadonowCrawler {


   public SaopauloSupermercadonowsantagemmaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "santa-gemma";
   }

   @Override
   protected String getSellerFullName() {
      return "Santa Gemma";
   }
}