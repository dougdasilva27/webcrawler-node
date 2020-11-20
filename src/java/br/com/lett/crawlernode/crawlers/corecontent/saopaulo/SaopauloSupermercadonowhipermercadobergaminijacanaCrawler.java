package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SupermercadonowCrawler;

public class SaopauloSupermercadonowhipermercadobergaminijacanaCrawler extends SupermercadonowCrawler {


   public SaopauloSupermercadonowhipermercadobergaminijacanaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "hipermercado-bergamini-jacana";
   }

   @Override
   protected String getSellerFullName() {
      return "Hipermercado Bergamini";
   }
}