package br.com.lett.crawlernode.crawlers.ranking.keywords.santoandre;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.models.GPAKeywordsCrawler;

public class SantoandrePaodeacucarCrawler extends GPAKeywordsCrawler {

   private static final String CEP1 = "09090-060";

   public SantoandrePaodeacucarCrawler(Session session) {
      super(session);
      this.cep = CEP1;
   }

}
