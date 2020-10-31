package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilRappiCrawler;

/**
 * Date: 19/10/20
 *
 * @author Fellype Layunne
 */
public class SaopauloRappistmarchepavaoCrawler extends BrasilRappiCrawler {
   private static final String STORE_TYPE = "st_marche_express";
   private static final String LOCATION = "lat=-23.6000459&lng=-46.6737028";
   private static final String STORE_ID = "900132997";

   public SaopauloRappistmarchepavaoCrawler(Session session) {
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
