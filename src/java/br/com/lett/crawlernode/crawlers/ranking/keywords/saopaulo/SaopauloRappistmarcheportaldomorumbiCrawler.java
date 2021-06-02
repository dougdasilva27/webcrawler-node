package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilRappiCrawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.RappiCrawler;

import java.util.Arrays;

public class SaopauloRappistmarcheportaldomorumbiCrawler extends BrasilRappiCrawler {

   public SaopauloRappistmarcheportaldomorumbiCrawler(Session session) {
      super(session);
      newUnification = true;
   }

   @Override
   protected String getStoreId() {
      return "900033773";
   }

}
