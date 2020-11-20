package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ArgentinaCarrefoursuper;

/**
 * Date: 2019-07-12
 * 
 * @author gabriel
 *
 */
public class ArgentinaCarrefoursuperCrawler extends ArgentinaCarrefoursuper {

   public ArgentinaCarrefoursuperCrawler(Session session) {
      super(session);
   }

   public static final String CEP = "1646";

   @Override
   protected String getCep() {
      return CEP;
   }


}
