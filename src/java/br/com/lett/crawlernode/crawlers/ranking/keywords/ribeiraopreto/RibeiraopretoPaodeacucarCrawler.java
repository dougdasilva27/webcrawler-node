package br.com.lett.crawlernode.crawlers.ranking.keywords.ribeiraopreto;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.models.GPAKeywordsCrawler;

public class RibeiraopretoPaodeacucarCrawler extends GPAKeywordsCrawler {

   private static final String CEP1 = "14025-230";

   public RibeiraopretoPaodeacucarCrawler(Session session) {
      super(session);
      this.cep = CEP1;
   }
}
