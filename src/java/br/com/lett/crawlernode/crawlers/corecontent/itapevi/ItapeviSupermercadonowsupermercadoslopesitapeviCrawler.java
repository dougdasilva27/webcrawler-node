package br.com.lett.crawlernode.crawlers.corecontent.itapevi;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SupermercadonowCrawler;

public class ItapeviSupermercadonowsupermercadoslopesitapeviCrawler extends SupermercadonowCrawler {


   public ItapeviSupermercadonowsupermercadoslopesitapeviCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercados-lopes-itapevi";
   }

   @Override
   protected String getSellerFullName() {
      return "Supermercados Lopes";
   }
}