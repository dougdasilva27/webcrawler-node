package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SavegnagoRanking;

public class BrasilSavegnagorioclaroCrawler extends SavegnagoRanking {

   public BrasilSavegnagorioclaroCrawler(Session session) {
      super(session);
   }

   @Override
   public String getStoreId() {
      return "13";
   }
}
