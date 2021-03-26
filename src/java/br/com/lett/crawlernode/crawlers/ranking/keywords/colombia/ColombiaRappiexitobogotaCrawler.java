package br.com.lett.crawlernode.crawlers.ranking.keywords.colombia;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.ColombiaRappiCrawlerRanking;

public class ColombiaRappiexitobogotaCrawler extends ColombiaRappiCrawlerRanking {

  public ColombiaRappiexitobogotaCrawler(Session session) {
    super(session);
  }

  public static final String STORE_ID = "6660081";
  public static final String STORE_TYPE = "hiper";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }

   @Override
   protected String getStoreType() {
      return STORE_TYPE;
   }
}
