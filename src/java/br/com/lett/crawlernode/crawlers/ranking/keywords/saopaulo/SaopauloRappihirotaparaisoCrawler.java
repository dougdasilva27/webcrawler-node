package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilRappiCrawlerRanking;

/**
 * Date: 19/10/20
 *
 * @author Fellype Layunne
 */
public class SaopauloRappihirotaparaisoCrawler extends BrasilRappiCrawlerRanking {

   public SaopauloRappihirotaparaisoCrawler(Session session) {
      super(session);
   }

   public static final String STORE_ID = "900021834";
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
