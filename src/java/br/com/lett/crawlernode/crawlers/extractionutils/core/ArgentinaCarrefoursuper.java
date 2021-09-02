package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import models.RatingsReviews;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.*;

/**
 * Date: 2019-08-28
 *
 * @author gabriel
 */
public abstract class ArgentinaCarrefoursuper extends CarrefourCrawler {

   private static final String SELLER_FULL_NAME = "CARREFOUR";

   protected ArgentinaCarrefoursuper(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.APACHE);
   }

   @Override
   protected String fetchPage(String url) {

      Map<String, String> headers = new HashMap<>();

      String token = getLocationToken();

      String userLocationData = getCep();
      headers.put("accept", "*/*");

      StringBuilder cookiesBuilder = new StringBuilder();
      if (token != null) {
         cookiesBuilder.append("vtex_segment=").append(token).append(";");
      }
      if (userLocationData != null) {
         cookiesBuilder.append("userLocationData=").append(userLocationData).append(";");
      }
      headers.put("cookie", cookiesBuilder.toString());

      Request request = RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setSendUserAgent(false)
         .mustSendContentEncoding(false)
         .setFetcheroptions(
            FetcherOptions.FetcherOptionsBuilder.create()
               .mustUseMovingAverage(false)
               .mustRetrieveStatistics(true)
               .build())
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_ANY_HAPROXY,
            ProxyCollection.LUMINATI_SERVER_BR,
            ProxyCollection.NO_PROXY)
         )
         .build();

      Response response = alternativeFetch(request);

      return response.getBody();
   }

   @Override
   protected String getHomePage() {
      return "https://www.carrefour.com.ar/";
   }

   @Override
   protected List<String> getMainSellersNames() {
      return Collections.singletonList(SELLER_FULL_NAME);
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }

}
