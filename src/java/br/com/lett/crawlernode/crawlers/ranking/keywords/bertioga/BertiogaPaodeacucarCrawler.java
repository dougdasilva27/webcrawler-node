package br.com.lett.crawlernode.crawlers.ranking.keywords.bertioga;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.models.GPAKeywordsCrawler;

public class BertiogaPaodeacucarCrawler extends GPAKeywordsCrawler {

   private static final String CEP1 = "11261-597";

   public BertiogaPaodeacucarCrawler(Session session) {
      super(session);
      this.cep = CEP1;
   }

}
