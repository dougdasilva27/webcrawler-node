package br.com.lett.crawlernode.crawlers.corecontent.ribeiraopreto;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.GPACrawler;

public class RibeiraopretoPaodeacucarCrawler extends GPACrawler {

   private static final String CEP1 = "14025-230";

   public RibeiraopretoPaodeacucarCrawler(Session session) {
      super(session);
      this.cep = CEP1;
   }
}
