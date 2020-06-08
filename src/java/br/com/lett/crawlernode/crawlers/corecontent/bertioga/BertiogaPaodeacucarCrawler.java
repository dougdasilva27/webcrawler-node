package br.com.lett.crawlernode.crawlers.corecontent.bertioga;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.GPACrawler;

public class BertiogaPaodeacucarCrawler extends GPACrawler {

   private static final String CEP1 = "11261-597";

   public BertiogaPaodeacucarCrawler(Session session) {
      super(session);
      this.cep = CEP1;
   }

}
