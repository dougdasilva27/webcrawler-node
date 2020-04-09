package br.com.lett.crawlernode.crawlers.corecontent.saobernardodocampo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.SupermercadonowCrawler;

public class SaobernardodocampoSupermercadonowsupermercadosdavosaobernardoCrawler extends SupermercadonowCrawler {


   public SaobernardodocampoSupermercadonowsupermercadosdavosaobernardoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercado-d-avo-sao-bernardo";
   }

   @Override
   protected String getSellerFullName() {
      return "Supermercados D'avó";
   }
}