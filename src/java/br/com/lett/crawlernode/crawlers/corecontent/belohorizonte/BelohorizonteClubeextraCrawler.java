package br.com.lett.crawlernode.crawlers.corecontent.belohorizonte;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.GPACrawler;

public class BelohorizonteClubeextraCrawler extends GPACrawler {

   private static final String CEP1 = "30150-221";

   public BelohorizonteClubeextraCrawler(Session session) {
      super(session);
      this.cep = CEP1;
   }
}
