package br.com.lett.crawlernode.crawlers.ranking.keywords.fortaleza;

import java.util.HashMap;
import java.util.Map;

import br.com.lett.crawlernode.crawlers.extractionutils.ranking.MerconnectRanking;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;

public class FortalezaCenterboxCrawler extends MerconnectRanking {

   public FortalezaCenterboxCrawler(Session session) {
      super(session);
   }


   private static final String CLIENT_ID = "dcdbcf6fdb36412bf96d4b1b4ca8275de57c2076cb9b88e27dc7901e8752cdff";
   private static final String CLIENT_SECRET = "27c92c098d3f4b91b8cb1a0d98138b43668c89d677b70bed397e6a5e0971257c";
   private static final String STORE_ID = "58";
   private static final String HOME_PAGE = "https://loja.centerbox.com.br/loja/58";


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
