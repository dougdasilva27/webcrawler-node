package br.com.lett.crawlernode.crawlers.corecontent.saocaetanodosul;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.GPACrawler;

public class SaocaetanodosulPaodeacucarCrawler extends GPACrawler {

   private static final String CEP1 = "09541-001";

   public SaocaetanodosulPaodeacucarCrawler(Session session) {
      super(session);
      this.cep = CEP1;
   }

}
