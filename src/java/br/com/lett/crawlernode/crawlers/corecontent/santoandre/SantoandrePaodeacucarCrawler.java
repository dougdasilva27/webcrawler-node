package br.com.lett.crawlernode.crawlers.corecontent.santoandre;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.GPACrawler;

public class SantoandrePaodeacucarCrawler extends GPACrawler {

   private static final String CEP1 = "09090-060";

   public SantoandrePaodeacucarCrawler(Session session) {
      super(session);
      this.cep = CEP1;
   }

}
