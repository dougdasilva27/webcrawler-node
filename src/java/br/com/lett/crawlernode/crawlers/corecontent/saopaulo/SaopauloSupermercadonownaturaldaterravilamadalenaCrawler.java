package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SupermercadonowCrawler;

public class SaopauloSupermercadonownaturaldaterravilamadalenaCrawler extends SupermercadonowCrawler {


   public SaopauloSupermercadonownaturaldaterravilamadalenaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "natural-da-terra-vila-madalena";
   }

   @Override
   protected String getSellerFullName() {
      return "Natural da Terra";
   }
}