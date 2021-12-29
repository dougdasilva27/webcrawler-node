package br.com.lett.crawlernode.crawlers.ranking.keywords.fortaleza;

import br.com.lett.crawlernode.crawlers.extractionutils.ranking.MerconnectRanking;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;

public class FortalezaCenterboxCrawler extends MerconnectRanking {

   public FortalezaCenterboxCrawler(Session session) {
      super(session);
   }


   private static final String CLIENT_ID = "dcdbcf6fdb36412bf96d4b1b4ca8275de57c2076cb9b88e27dc7901e8752cdff";
   private static final String CLIENT_SECRET = "27c92c098d3f4b91b8cb1a0d98138b43668c89d677b70bed397e6a5e0971257c";


   @Override
   protected String getClientId() {
      return CLIENT_ID;
   }

   @Override
   protected String getClientSecret() {
      return CLIENT_SECRET;
   }

   @Override
   protected String getHomePage() {
      return session.getOptions().optString("HOME_PAGE");
   }

   @Override
   protected String getStoreId() {
      return session.getOptions().optString("STORE_ID");
   }

   @Override
   protected String retrieveUrl(JSONObject product) {

      return getHomePage() + "/categoria/" + product.optString("section_id") + "/produto/" + product.optString("id") + "?origin=searching";
   }

}
