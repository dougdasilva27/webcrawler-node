package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JavanetDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CrawlerUtils;
import cdjd.com.dremio.exec.rpc.ResponseSender;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.awt.geom.RectangularShape;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class BrasilSamsclubCrawler extends VTEXGraphQLRanking {
   private String HOME_PAGE = getHomePage();

   private String getHomePage() {
      return session.getOptions().optString("homePage");
   }

   public BrasilSamsclubCrawler(Session session) {
      super(session);
   }

   @Override
   protected JSONObject fetchSearchApi(Document doc, String redirect) {
      JSONObject searchApi = new JSONObject();
      StringBuilder url = new StringBuilder();
      String sha256Hash = getSha256Hash(doc);
      url.append(HOME_PAGE).append("_v/segment/graphql/v1?");
      JSONObject extensions = new JSONObject();
      JSONObject persistedQuery = new JSONObject();

      persistedQuery.put("version", "1");
      persistedQuery.put("sha256Hash", sha256Hash);
      persistedQuery.put("sender", "vtex.store-resources@0.x");
      persistedQuery.put("provider", "vtex.search-graphql@0.x");

      extensions.put("variables", createVariablesBase64(redirect));
      extensions.put("persistedQuery", persistedQuery);

      url.append("extensions=").append(URLEncoder.encode(extensions.toString(), StandardCharsets.UTF_8));
      this.log("Link onde são feitos os crawlers:" + url);

      Request request = Request.RequestBuilder.create()
         .setUrl(url.toString())
         .setCookies(cookies)
         .setProxyservice(List.of(
            ProxyCollection.BUY,
            ProxyCollection.LUMINATI_SERVER_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_ROTATE_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_BR))
         .build();

      String strResp = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new JsoupDataFetcher(), new FetcherDataFetcher(), new JavanetDataFetcher()), session, "get").getBody();
      JSONObject response = CrawlerUtils.stringToJson(strResp);

      if (response.has("data") && !response.isNull("data")) {
         JSONObject data = response.optJSONObject("data");

         if (data.has("productSearch") && !data.isNull("productSearch")) {
            searchApi = data.optJSONObject("productSearch");

            if (searchApi.optString("redirect") != null && !searchApi.optString("redirect").isEmpty()) {
               searchApi = fetchSearchApi(doc, searchApi.optString("redirect"));
            }
         }
      }

      return searchApi;
   }
}
