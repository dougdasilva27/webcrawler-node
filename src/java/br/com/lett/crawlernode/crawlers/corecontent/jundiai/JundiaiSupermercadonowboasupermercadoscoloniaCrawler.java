package br.com.lett.crawlernode.crawlers.corecontent.jundiai;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.SupermercadonowCrawler;

public class JundiaiSupermercadonowboasupermercadoscoloniaCrawler extends SupermercadonowCrawler {


   public JundiaiSupermercadonowboasupermercadoscoloniaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "boa-supermercados-jundiai-colonia";
   }

   @Override
   protected String getSellerFullName() {
      return "Boa Supermercados";
   }
}