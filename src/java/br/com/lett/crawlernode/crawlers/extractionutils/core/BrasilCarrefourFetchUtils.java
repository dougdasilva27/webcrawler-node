package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BrasilCarrefourFetchUtils {

   public static String fetchPage(String url, String token, String userLocationData, Session session) {

      Map<String, String> headers = new HashMap<>();

      headers.put("accept", "*/*");

      StringBuilder cookiesBuilder = new StringBuilder();
      if (token != null) {
         cookiesBuilder.append("vtex_segment=").append(token).append(";");
      }
      if (userLocationData != null) {
         cookiesBuilder.append("userLocationData=").append(userLocationData).append(";");
      }
      headers.put("cookie", cookiesBuilder.toString());

      Request request = Request.RequestBuilder.create()
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
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.BUY,
            ProxyCollection.NETNUT_RESIDENTIAL_MX,
            ProxyCollection.NO_PROXY)
         )
         .build();

      return alternativeFetch(request, session).getBody();
   }

   private static Response alternativeFetch(Request request, Session session) {
      List<DataFetcher> dataFetchers = Arrays.asList(new ApacheDataFetcher(), new JsoupDataFetcher());

      Response response = null;

      for (DataFetcher localDataFetcher : dataFetchers) {
         response = localDataFetcher.get(session, request);
         if (checkResponse(response)) {
            return response;
         }
      }

      return response;
   }

   private static boolean checkResponse(Response response) {
      int statusCode = response.getLastStatusCode();

      return (Integer.toString(statusCode).charAt(0) == '2'
         || Integer.toString(statusCode).charAt(0) == '3'
         || statusCode == 404);
   }


}
