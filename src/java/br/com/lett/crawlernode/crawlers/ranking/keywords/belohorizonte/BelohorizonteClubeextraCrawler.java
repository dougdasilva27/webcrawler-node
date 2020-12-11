package br.com.lett.crawlernode.crawlers.ranking.keywords.belohorizonte;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.models.GPAKeywordsCrawler;

public class BelohorizonteClubeextraCrawler extends GPAKeywordsCrawler {

   private static final String CEP1 = "30150-221";

   public BelohorizonteClubeextraCrawler(Session session) {
      super(session);
      this.cep = CEP1;
   }
}
