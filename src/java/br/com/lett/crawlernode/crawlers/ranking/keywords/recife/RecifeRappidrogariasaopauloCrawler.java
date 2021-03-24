package br.com.lett.crawlernode.crawlers.ranking.keywords.recife;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilRappiCrawlerRanking;

public class RecifeRappidrogariasaopauloCrawler extends BrasilRappiCrawlerRanking {
   public RecifeRappidrogariasaopauloCrawler(Session session) {
      super(session);
      newUnification = true;
   }

   @Override
   protected String getStoreId() {
      return "900153585";
   }

   @Override
   protected String getStoreType() {
      return "sao_paulo";
   }
}
