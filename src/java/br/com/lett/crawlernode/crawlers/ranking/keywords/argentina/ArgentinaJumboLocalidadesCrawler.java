package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.VtexRankingKeywordsNew;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ArgentinaJumboLocalidadesCrawler extends VtexRankingKeywordsNew {

   public ArgentinaJumboLocalidadesCrawler(Session session) {
      super(session);
   }

   @Override
   protected String setHomePage() {
      return session.getOptions().optString("homePage");
   }

   @Override
   protected void processBeforeFetch() {
      JSONObject cookiesOptions = session.getOptions().optJSONObject("cookies");
      if (cookiesOptions != null) {
         for (String key : cookiesOptions.keySet()) {
            cookies.add(new BasicClientCookie(key, cookiesOptions.optString(key)));
         }
      }
   }

   @Override
   protected JSONArray fetchPage(String url) {

      url = setHomePage() + "/api/catalog_system/pub/products/search/" + "?&&ft=" + keywordEncoded.replace("+", "%20")
         + "&O=OrderByScoreDESC&_from=" + ((currentPage - 1) * 18) + "&_to=" + ((currentPage) * 18);

      Map<String, String> headers = new HashMap<>();
      if (cookies != null && !cookies.isEmpty()) {
         headers.put("cookie", CommonMethods.cookiesToString(cookies));
      }

      Request request = Request.RequestBuilder.create()
         .setHeaders(headers)
         .setUrl(url)
         .build();

      Response response = dataFetcher.get(session, request);

      return CrawlerUtils.stringToJsonArray(response.getBody());
   }

}
