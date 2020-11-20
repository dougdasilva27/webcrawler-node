package br.com.lett.crawlernode.crawlers.corecontent.taboaodaserra;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SupermercadonowCrawler;

public class TaboaodaserraSupermercadonowsupermercadosdavotaboaodaserraCrawler extends SupermercadonowCrawler {


   public TaboaodaserraSupermercadonowsupermercadosdavotaboaodaserraCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercado-d-avo-taboao-da-serra";
   }

   @Override
   protected String getSellerFullName() {
      return "Supermercados D'avó";
   }
}