package br.com.lett.crawlernode.crawlers.corecontent.pitangueiras;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SupermercadonowCrawler;

public class PitangueirasSupermercadonowhirotasupermercadossaudeCrawler extends SupermercadonowCrawler {


   public PitangueirasSupermercadonowhirotasupermercadossaudeCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercado-hirota-saude-jabaquara";
   }

   @Override
   protected String getSellerFullName() {
      return "Hirota Supermercados";
   }
}