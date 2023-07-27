package br.com.lett.crawlernode.core.fetcher.methods;


import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.main.GlobalConfigurations;

import java.io.File;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class HttpClientFetcher implements DataFetcher {
   @Override
   public Response get(Session session, Request request) {
      try {
         return HttpClientRequest(session, request, "GET_REQUEST", false);
      } catch (Exception e) {
         return null;
      }
   }


   @Override
   public Response post(Session session, Request request) {
      try {
         return HttpClientRequest(session, request, "POST_REQUEST", false);
      } catch (Exception e) {
         return null;
      }

   }

   @Override
   public File fetchImage(Session session, Request request) {
      return null;
   }

   private Response HttpClientRequest(Session session, Request request, String getRequest, boolean b) {
      try {
         HttpResponse<String> response = null;
         int countProxyList = 0;
         int totalCountProxy = 0;
         do {
            HttpClient.Builder httpClientBuilder = HttpClient.newBuilder();
            if (request.isFollowRedirects()) {
               httpClientBuilder = httpClientBuilder.followRedirects(HttpClient.Redirect.NORMAL);
            }
            if (request.getProxyServices() != null) {
               totalCountProxy = request.getProxyServices().size();
               httpClientBuilder = setProxys(httpClientBuilder, request, countProxyList);
               countProxyList++;
            }
            HttpClient httpClient = httpClientBuilder.build();

            HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder()
               .uri(URI.create(request.getUrl()));

            if (getRequest.equals("GET_REQUEST")) {
               httpRequestBuilder.GET();
            } else {
               httpRequestBuilder.POST(HttpRequest.BodyPublishers.ofString(request.getPayload()));
            }
            if (request.getHeaders().size() > 0) {
               httpRequestBuilder.headers(listHeaders(request));
            }
            HttpRequest httpRequest = httpRequestBuilder.build();

            response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

         } while (countProxyList < totalCountProxy && response.statusCode() != 200);


         return new Response.ResponseBuilder()
            .setBody(response.body())
            .setLastStatusCode(response.statusCode())
            .build();
      } catch (Exception e) {
         // Tratamento de erros
         e.printStackTrace();
      }

      return null;

   }

   private HttpClient.Builder setProxys(HttpClient.Builder httpClientBuilder, Request request, int countProxyList) {
      if (countProxyList < request.getProxyServices().size()) {
         String proxyHost = GlobalConfigurations.proxies.getProxy(request.getProxyServices().get(countProxyList)).get(0).getAddress();
         int proxyPort = GlobalConfigurations.proxies.getProxy(request.getProxyServices().get(countProxyList)).get(0).getPort();
         httpClientBuilder = httpClientBuilder.proxy(ProxySelector.of(new InetSocketAddress(proxyHost, proxyPort)));

      }

      return httpClientBuilder;
   }


   private String[] listHeaders(Request request) {
      String[] listaHeaders = new String[request.getHeaders().size() * 2];
      Map<String, String> headers = request.getHeaders();
      int i = 0;
      for (String key : headers.keySet()) {
         listaHeaders[i++] = key;
         listaHeaders[i++] = headers.get(key);
      }
      return listaHeaders;
   }
}
