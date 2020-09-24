package br.com.lett.crawlernode.crawlers.corecontent.jundiai;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.SupermercadonowCrawler;

public class JundiaiSupermercadonowboasupermercadosCrawler extends SupermercadonowCrawler {

   public JundiaiSupermercadonowboasupermercadosCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "boa-supermercados-jundiai-quarto-centenario";
   }

   @Override
   protected String getSellerFullName() {
      return "Boa Supermercados";
   }
}
