package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilRappiCrawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.RappiCrawler;

public class SaopauloRappidrogasilitainCrawler extends BrasilRappiCrawler {


   public SaopauloRappidrogasilitainCrawler(Session session) {
      super(session);
      newUnification =true;
   }

   @Override
   protected String getStoreId() {
      return "900005065";
   }


}
