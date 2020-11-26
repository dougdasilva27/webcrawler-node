package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions.FetcherOptionsBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.B2WCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

public class SaopauloAmericanasCrawler extends B2WCrawler {

   private static final String HOME_PAGE = "https://www.americanas.com.br/";
   private static final String MAIN_SELLER_NAME_LOWER = "americanas.com";
   private static final int RATING_API_VERSION = 1;
   private static final String KEY_SHA_256 = "3a716c1d89ae750c969fadfecf3d88e8057880358c57ddc29f3255083cdd6505";

   public SaopauloAmericanasCrawler(Session session) {
      super(session);
      super.subSellers = Arrays.asList("b2w", "lojas americanas", "lojas americanas mg", "lojas americanas rj", "lojas americanas sp", "lojas americanas rs");
      super.sellerNameLower = MAIN_SELLER_NAME_LOWER;
      super.homePage = HOME_PAGE;
   }

   @Override
   public void handleCookiesBeforeFetch() {
      Request request;

      if (dataFetcher instanceof FetcherDataFetcher) {
         request = RequestBuilder.create().setUrl(HOME_PAGE)
               .setCookies(cookies)
               .setProxyservice(
                     Arrays.asList(
                           ProxyCollection.INFATICA_RESIDENTIAL_BR,
                           ProxyCollection.NETNUT_RESIDENTIAL_BR,
                           ProxyCollection.BUY
                     )
               ).mustSendContentEncoding(false)
               .setFetcheroptions(FetcherOptionsBuilder.create()
                     .setForbiddenCssSelector("#px-captcha")
                     .mustUseMovingAverage(false)
                     .mustRetrieveStatistics(true).build())
               .build();
      } else {
         request = RequestBuilder.create().setUrl(HOME_PAGE).setCookies(cookies).build();
      }

      this.cookies = CrawlerUtils.fetchCookiesFromAPage(request, "www.americanas.com.br", "/", null, session, dataFetcher);
   }

   @Override
   protected RatingsReviews crawlRatingReviews(JSONObject frontPageJson, String skuInternalPid) {
      RatingsReviews ratingReviews = new RatingsReviews();
      JSONObject rating = fetchRatingApi(skuInternalPid);

      JSONObject data = rating.optJSONObject("data");

      if (data != null) {

         JSONObject product = data.optJSONObject("product");

         if (product != null) {

            JSONObject ratingInfo = product.optJSONObject("rating");

            if (ratingInfo != null) {
               ratingReviews.setTotalWrittenReviews(ratingInfo.optInt("reviews", 0));
               ratingReviews.setTotalRating(ratingInfo.optInt("reviews", 0));
               ratingReviews.setAverageOverallRating(ratingInfo.optDouble("average", 0d));
            } else {
               ratingReviews.setTotalWrittenReviews(0);
               ratingReviews.setTotalRating(0);
               ratingReviews.setAverageOverallRating(0.0);
            }
         }
      }

      return ratingReviews;
   }

   private JSONObject fetchRatingApi(String internalId) {
      StringBuilder url = new StringBuilder();
      url.append("https://catalogo-bff-v1-americanas.b2w.io/graphql?");

      JSONObject variables = new JSONObject();
      JSONObject extensions = new JSONObject();
      JSONObject persistedQuery = new JSONObject();

      variables.put("productId", internalId);
      variables.put("offset", 5);

      persistedQuery.put("version", RATING_API_VERSION);
      persistedQuery.put("sha256Hash", KEY_SHA_256);

      extensions.put("persistedQuery", persistedQuery);

      StringBuilder payload = new StringBuilder();
      payload.append("operationName=productReviews");
      payload.append("&device=desktop");
      payload.append("&oneDayDelivery=undefined");
      try {
         payload.append("&variables=" + URLEncoder.encode(variables.toString(), "UTF-8"));
         payload.append("&extensions=" + URLEncoder.encode(extensions.toString(), "UTF-8"));
      } catch (UnsupportedEncodingException e) {
         Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
      }
      url.append(payload.toString());

      Request request = Request.RequestBuilder.create()
            .setUrl(url.toString())
            .build();

      return CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
   }
}
