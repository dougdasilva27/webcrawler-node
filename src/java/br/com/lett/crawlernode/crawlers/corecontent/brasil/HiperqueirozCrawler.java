package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.MerconnectCrawler;

public class HiperqueirozCrawler extends MerconnectCrawler {

   private static final String CLIENT_ID = "dcdbcf6fdb36412bf96d4b1b4ca8275de57c2076cb9b88e27dc7901e8752cdff";
   private static final String CLIENT_SECRET = "27c92c098d3f4b91b8cb1a0d98138b43668c89d677b70bed397e6a5e0971257c";
   private static final String STORE_ID = "167";

   public HiperqueirozCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getClientId() {
      return CLIENT_ID;
   }

   @Override
   protected String getClientSecret() {
      return CLIENT_SECRET;
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
