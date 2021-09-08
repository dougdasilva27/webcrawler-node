package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.CNOVANewCrawler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SaopauloPontofrioCrawler extends CNOVANewCrawler {

   private static final String STORE = "pontofrio";
   private static final String INITIALS = "PF";
   private static final List<String> SELLER_NAMES = Arrays.asList("Pontofrio", "pontofrio.com");

   public SaopauloPontofrioCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.FETCHER);
   }

   @Override
   protected String getStore() {
      return STORE;
   }

   @Override
   protected List<String> getSellerName() {
      return SELLER_NAMES;
   }

   @Override
   protected String getInitials() {
      return INITIALS;
   }

   @Override
   protected Response fetchPage(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
      headers.put("accept-encoding", "");
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6");
      headers.put("cache-control", "no-cache");
      headers.put("pragma", "no-cache");
      headers.put("sec-fetch-dest", "document");
      headers.put("sec-fetch-mode", "navigate");
      headers.put("sec-fetch-site", "none");
      headers.put("sec-fetch-user", "?1");
      headers.put("upgrade-insecure-requests", "1");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .setFetcheroptions(FetcherOptions.FetcherOptionsBuilder.create()
            .mustUseMovingAverage(false)
            .mustRetrieveStatistics(true)
            .build())
         .setHeaders(headers)
         .setProxyservice(
            Arrays.asList(
               ProxyCollection.NETNUT_RESIDENTIAL_ANY_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_US_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY

            )
         )
         .build();


      return alternativeFetch(request);
   }


   private Response alternativeFetch(Request request) {
      List<DataFetcher> httpClients = Arrays.asList(new JsoupDataFetcher(), new FetcherDataFetcher());

      Response response = null;

      for (DataFetcher localDataFetcher : httpClients) {
         response = localDataFetcher.get(session, request);
         if (checkResponse(response)) {
            return response;
         }
      }

      return response;
   }

   boolean checkResponse(Response response) {
      int statusCode = response.getLastStatusCode();

      return (Integer.toString(statusCode).charAt(0) == '2'
         || Integer.toString(statusCode).charAt(0) == '3'
         || statusCode == 404);
   }


}
