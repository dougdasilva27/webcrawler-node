package br.com.lett.crawlernode.crawlers.ranking.keywords.fortaleza;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.MerconnectCrawler;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.MerconnectRanking;
import org.json.JSONObject;

public class FortalezaArmazemfortaliCrawler extends MerconnectRanking {


   private static final String CLIENT_ID = "dcdbcf6fdb36412bf96d4b1b4ca8275de57c2076cb9b88e27dc7901e8752cdff";
   private static final String CLIENT_SECRET = "27c92c098d3f4b91b8cb1a0d98138b43668c89d677b70bed397e6a5e0971257c";
   private static final String STORE_ID = "71";
   private static final String HOME_PAGE = "https://armazemfortali.com.br/loja/71";

   public FortalezaArmazemfortaliCrawler(Session session) {
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
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }

   @Override
   protected String retrieveUrl(JSONObject product) {
      return getHomePage() + "/categoria/" + product.optString("section_id") + "/produto/" + product.optString("id") + "?origin=searching";
   }

}
