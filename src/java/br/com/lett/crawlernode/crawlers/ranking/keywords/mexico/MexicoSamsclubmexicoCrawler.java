package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.MexicoSamsclubCrawler;

public class MexicoSamsclubmexicoCrawler extends MexicoSamsclubCrawler {

   private static final String STORE_ID = "_=1559844888311";

   public MexicoSamsclubmexicoCrawler(Session session) {
      super(session);
      super.setStoreId(STORE_ID);
   }
}
