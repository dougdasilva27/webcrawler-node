package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;

import javax.xml.crypto.Data;
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

   public static String getRegionId(DataFetcher dataFetcher, String cep, Session session) {
      if(cep == null) return null;

      String regionApiUrl = "https://mercado.carrefour.com.br/api/checkout/pub/regions?country=BRA&postalCode="+ cep + "&sc=2";
      Map<String, String> headers = new HashMap<>();
      headers.put("authority", "mercado.carrefour.com.br");
      headers.put("sec-ch-ua", "\" Not A;Brand\";v=\"99\", \"Chromium\";v=\"96\", \"Google Chrome\";v=\"96\"");
      headers.put("sec-ch-ua-mobile", "?0");
      headers.put("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.93 Safari/537.36");
      headers.put("sec-ch-ua-platform", "\"Windows\"");
      headers.put("accept", "*/*");
      headers.put("sas-fetch-site", "same-origin");
      headers.put("sas-fetch-mode", "cors");
      headers.put("sas-fetch-dest", "empty");
      headers.put("referer", "https://mercado.carrefour.com.br/");

      Request request = Request.RequestBuilder.create().setUrl(regionApiUrl).setHeaders(headers).build();
      String response = dataFetcher.get(session, request).getBody();
      JSONArray responseJSON = JSONUtils.stringToJsonArray(response);

      return responseJSON.getJSONObject(0) != null ? responseJSON.getJSONObject(0).optString("id") : null;
   }


}
