package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilRappiCrawler;

/**
 * Date: 19/10/20
 *
 * @author Fellype Layunne
 */
public class SaopauloRappihirotaparaisoCrawler extends BrasilRappiCrawler {
   private static final String STORE_TYPE = "hirota_super";
   private static final String LOCATION = "lat=-23.5764984&lng=-46.6495648";
   private static final String STORE_ID = "900021834";

   public SaopauloRappihirotaparaisoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String setStoreType() {
      return STORE_TYPE;
   }

   @Override
   protected String setStoreId() {
      return STORE_ID;
   }

   @Override
   protected String setLocationParameters() {
      return LOCATION;
   }
}
