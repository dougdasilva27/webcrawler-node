package br.com.lett.crawlernode.crawlers.corecontent.santamaria;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SupermercadonowCrawler;

public class SantamariaSupermercadonowbeltramesantamariaCrawler extends SupermercadonowCrawler {


   public SantamariaSupermercadonowbeltramesantamariaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "beltrame-supermercados-santa-maria";
   }

   @Override
   protected String getSellerFullName() {
      return "Beltrame";
   }
}