package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilRappiCrawlerRanking;

public class SaopauloRappidrogariasaopauloipirangaCrawler extends BrasilRappiCrawlerRanking {
   public SaopauloRappidrogariasaopauloipirangaCrawler(Session session) {
      super(session);
      newUnification = true;
   }

   @Override
   protected String getStoreId() {
      return "900170932";
   }

   @Override
   protected String getStoreType() {
      return "sao_paulo";
   }
}
