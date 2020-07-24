package br.com.lett.crawlernode.crawlers.ranking.keywords.saocaetanodosul;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.models.GPAKeywordsCrawler;

public class SaocaetanodosulPaodeacucarCrawler extends GPAKeywordsCrawler {

   private static final String CEP1 = "09541-001";

   public SaocaetanodosulPaodeacucarCrawler(Session session) {
      super(session);
      this.cep = CEP1;
   }

}
