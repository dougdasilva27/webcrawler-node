
package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXNewScraper;
import br.com.lett.crawlernode.exceptions.ApiResponseException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class BrasilConsulCrawler extends VTEXNewScraper {

   private static final String HOME_PAGE = "https://loja.consul.com.br/";
   private static final List<String> SELLERS = Arrays.asList("Whirlpool", "Consul", "Brastemp");

   public BrasilConsulCrawler(Session session) {
      super(session);
   }

   @Override
   public void handleCookiesBeforeFetch() {
      String vtex_segment = session.getOptions().optString("vtex_segment");
      BasicClientCookie cookie = new BasicClientCookie("vtex_segment", vtex_segment);
      this.cookies.add(cookie);
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }


   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected List<String> getMainSellersNames() {
      return SELLERS;
   }


   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject apiJson) {

      JSONObject apiTrustVox = fetchSearchApiTrustVoxVtex(internalPid);
      RatingsReviews ratingsReviews = new RatingsReviews();
      if (!apiTrustVox.isEmpty()) {

         ratingsReviews.setInternalId(internalId);
         ratingsReviews.setAverageOverallRating(apiTrustVox.optDouble("average"));
         ratingsReviews.setTotalRating(apiTrustVox.optInt("count"));
      }

      return ratingsReviews;
   }


   protected JSONObject fetchSearchApiTrustVoxVtex(String internalPid) {
      JSONObject searchApi = new JSONObject();
      JSONObject productId = new JSONObject();
      productId.put("productId", internalPid);
      StringBuilder url = new StringBuilder();
      url.append(HOME_PAGE + "_v/segment/graphql/v1?");

      JSONObject extensions = new JSONObject();
      JSONObject persistedQuery = new JSONObject();

      persistedQuery.put("version", "1");
      persistedQuery.put("sha256Hash", "9230c4c255f4e6696f429eb410a88439ddf45a97e6dd2a208d4afc79d9fdb77d");
      persistedQuery.put("sender", "consul.trustvox@0.x");
      persistedQuery.put("provider", "consul.store-graphql@0.x");

      extensions.put("variables", Base64.getEncoder().encodeToString(productId.toString().getBytes()));
      extensions.put("persistedQuery", persistedQuery);

      StringBuilder payload = new StringBuilder();
      payload.append("workspace=master");
      payload.append("&maxAge=short");
      payload.append("&appsEtag=remove");
      payload.append("&domain=store");
      payload.append("&locale=pt-BR");
      payload.append("&operationName=productRatingCount");
      payload.append("&variables=" + URLEncoder.encode("{}", StandardCharsets.UTF_8));
      payload.append("&extensions=" + URLEncoder.encode(extensions.toString(), StandardCharsets.UTF_8));
      url.append(payload);

      Request request = Request.RequestBuilder.create()
         .setUrl(url.toString())
         .build();

      JSONObject response = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
      try {
         if (!response.has("errors")) {
            searchApi = JSONUtils.getValueRecursive(response, "data.productRatingCount.products_rates.0", JSONObject.class, new JSONObject());
         } else {
            throw new ApiResponseException(response.toString());
         }
      } catch (Exception ex) {
         Logging.printLogError(logger, session, CommonMethods.getStackTrace(ex));
      }

      return searchApi;
   }

}
