package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.models.GPAKeywordsCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.apache.http.client.utils.URIBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PaodeacucarKeywordsImpl extends LinxImpulseRanking {

   private boolean isRedirection = false;

   public PaodeacucarKeywordsImpl(Session session) {
      super(session);
   }

   @Override
   protected String crawlInternalId(JSONObject product, String internalPid) {String internalId = internalPid;
      String url = product.optString("url");
      Pattern pattern = Pattern.compile("/([0-9]+)");
      Matcher matcher = pattern.matcher(url);
      if (matcher.find()) {
         internalId = matcher.group(1);
      }
      return internalId;
   }
   @Override
   protected JSONObject fetchPage(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("origin", this.homePage);

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build();

      Response response = dataFetcher.get(session, request);

      JSONObject jsonObject = CrawlerUtils.stringToJSONObject(response.getBody());

      if (jsonObject.optString("queryType").equals("redirect")) {
         isRedirection = true;
         String linkRedirection = jsonObject.optString("link");
         this.keywordEncoded = getRedirectionKeyword(linkRedirection);

         jsonObject = fetchJSONObject(mountURL());

      }

      return jsonObject;
   }

   @Override
   protected String mountURL() {
      URIBuilder uriBuilder = new URIBuilder()
         .setScheme("https")
         .setHost("api.linximpulse.com")
         .addParameter("apiKey", this.apiKey)
         .addParameter("origin", this.homePage)
         .addParameter("page", String.valueOf(this.currentPage))
         .addParameter("resultsPerPage", String.valueOf(this.pageSize))
         .addParameter("sortBy", this.sortBy)
         .addParameter("showOnlyAvailable", String.valueOf(this.showOnlyAvailable));

      if (this.salesChannel != null && !this.salesChannel.isEmpty()) {
         salesChannel.forEach(channel -> uriBuilder.addParameter("salesChannel", channel));
      }

      if (isRedirection) {
         uriBuilder.setPath("/engage/search/v3/hotsites");
         uriBuilder.addParameter("name", this.keywordEncoded);
      } else {
         uriBuilder.setPath("/engage/search/v3/search");
         uriBuilder.addParameter("terms", this.keywordEncoded);
      }

      return uriBuilder.toString();
   }

   private String getRedirectionKeyword(String link) {
      if (link == null) return null;
      Pattern pattern = Pattern.compile("especial\\/(.*)\\\\?");
      Matcher matcher = pattern.matcher(link);
      if (matcher.find()) {
         return matcher.group(1);
      }

      return null;
   }

}
