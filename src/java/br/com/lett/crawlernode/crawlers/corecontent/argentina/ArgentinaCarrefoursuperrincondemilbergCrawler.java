package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.ArgentinaCarrefoursuper;

/**
 * Date: 2019-07-12
 * 
 * @author gabriel
 *
 */
public class ArgentinaCarrefoursuperrincondemilbergCrawler extends ArgentinaCarrefoursuper {

   public ArgentinaCarrefoursuperrincondemilbergCrawler(Session session) {
      super(session);
   }

   public static final String CEP = "1648";

   @Override
   protected String getCep() {
      return CEP;
   }


}
