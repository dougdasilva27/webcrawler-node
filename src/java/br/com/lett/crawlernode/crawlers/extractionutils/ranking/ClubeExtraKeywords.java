package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.client.utils.URIBuilder;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClubeExtraKeywords extends LinxImpulseRanking {

   private boolean isRedirection = false;

   public ClubeExtraKeywords(Session session) {
      super(session);
   }

   @Override
   protected String crawlInternalId(JSONObject product, String internalPid) {
      String url = super.crawlProductUrl(product);
      if (url != null && !url.isEmpty()) {
         String internalId = CommonMethods.getLast(url.split("/"));
         if (internalId != null && !internalId.isEmpty()) {
            return internalId;
         }
      }
      return null;
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
      String kw = "";
      Pattern pattern = Pattern.compile("especial\\/(.*)\\?");
      Matcher matcher = pattern.matcher(link);
      if (matcher.find()) {
         kw = matcher.group(1);
      }

      return kw;
   }


}
