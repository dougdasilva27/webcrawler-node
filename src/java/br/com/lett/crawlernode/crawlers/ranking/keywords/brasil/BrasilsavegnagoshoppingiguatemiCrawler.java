package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SavegnagoRanking;

public class BrasilsavegnagoshoppingiguatemiCrawler extends SavegnagoRanking {
   public BrasilsavegnagoshoppingiguatemiCrawler(Session session) {
      super(session);
   }

   @Override
   public String getStoreId() {
      return "1";
   }
}
