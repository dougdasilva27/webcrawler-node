package br.com.lett.crawlernode.core.fetcher.methods;


import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.main.GlobalConfigurations;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
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

   private Response HttpClientRequest(Session session, Request request, String getRequest, boolean b) throws IOException, InterruptedException {
      String proxyHost = GlobalConfigurations.proxies.getProxy(request.getProxyServices().get(0)).get(0).getAddress();
      int proxyPort = GlobalConfigurations.proxies.getProxy(request.getProxyServices().get(0)).get(0).getPort();

      HttpClient httpClient = HttpClient.newBuilder()
         .proxy(ProxySelector.of(new InetSocketAddress(proxyHost, proxyPort)))
         .build();

      HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder()
         .uri(URI.create(request.getUrl()));

      if (getRequest.equals("GET_REQUEST")) {
         httpRequestBuilder.GET();
      } else {
         httpRequestBuilder.POST(HttpRequest.BodyPublishers.ofString(request.getPayload()));
      }
      if (request.getHeaders().size() > 0) {
         httpRequestBuilder.headers(listaHeaders(request));
      }

      HttpRequest httpRequest = httpRequestBuilder.build();

      try {
         // Envio da requisição
         HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

         // Obtenção da resposta
         int statusCode = response.statusCode();
         String responseBody = response.body();
         HttpHeaders headers = response.headers();

         // Exemplo de uso dos dados obtidos
         System.out.println("Status Code: " + statusCode);
         System.out.println("Response Body: " + responseBody);
         System.out.println("Headers: " + headers);
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

   private String[] listaHeaders(Request request) {
      String[] listaHeaders = new String[request.getHeaders().size()*2];
      Map<String, String> headers = request.getHeaders();
      int i = 0;
      for (String chave : headers.keySet()) {
         listaHeaders[i++] = chave;
         listaHeaders[i++] = headers.get(chave);
      }
      return listaHeaders;
   }
}
