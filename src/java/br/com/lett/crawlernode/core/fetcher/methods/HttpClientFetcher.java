package br.com.lett.crawlernode.core.fetcher.methods;


import br.com.lett.crawlernode.core.fetcher.models.LettProxy;
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
import java.util.Collections;
import java.util.List;

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
      String proxyUsername = GlobalConfigurations.proxies.getProxy(request.getProxyServices().get(0)).get(0).getUser();
      String proxyPassword = GlobalConfigurations.proxies.getProxy(request.getProxyServices().get(0)).get(0).getPass();

      String url = request.getUrl();
      //HttpClient client = HttpClient.newBuilder().proxy(ProxySelector.of(new InetSocketAddress("haproxy.lett.global", port))).build();
      // Configuração do cliente HTTP
      HttpClient httpClient = HttpClient.newBuilder()
         .proxy(ProxySelector.of(new InetSocketAddress(proxyHost, proxyPort)))
        // .authenticator(new ProxyAuthenticator(proxyUsername, proxyPassword))
       .followRedirects(HttpClient.Redirect.NORMAL)
         .build();

      // Criação da requisição
      HttpRequest requestHttp = HttpRequest.newBuilder()
         .header("Proxy-Authorization", getBasicAuthenticationHeader(proxyUsername, proxyPassword))
         .uri(URI.create(url))
         .build();

      try {
         // Envio da requisição
         HttpResponse<String> response = httpClient.send(requestHttp, HttpResponse.BodyHandlers.ofString());

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


//
//      ProxySelector proxySelector = new ProxySelector() {
//         @Override
//         public List<Proxy> select(URI uri) {
//            return Collections.singletonList(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort)));
//         }
//
//         @Override
//         public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
//            System.out.println("Failed to connect to proxy: " + proxyHost);
//         }
//      };
//
//      Authenticator authenticator = new Authenticator() {
//         @Override
//         protected PasswordAuthentication getPasswordAuthentication() {
//
//            return new PasswordAuthentication(proxyUsername, proxyPassword.toCharArray());
//
//         }
//
//      };
//
//      Authenticator.setDefault(authenticator);
//
//
//      HttpClient client = HttpClient.newBuilder()
//         .proxy(proxySelector)
//         //.proxy(ProxySelector.of(new InetSocketAddress("haproxy.lett.global", port)))
//         .build();
//
//
//      HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder()
//         .uri(URI.create(request.getUrl()));
//      if (getRequest.equals("GET_REQUEST")) {
//         httpRequestBuilder.GET();
//      } else {
//         httpRequestBuilder.POST(HttpRequest.BodyPublishers.ofString(request.getPayload()));
//      }
//      httpRequestBuilder = Proxyhandler(httpRequestBuilder, request);
//      HttpRequest httpRequest = httpRequestBuilder.build();
//
//      HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
//
//      return new Response.ResponseBuilder()
//         .setBody(response.body())
//         .setLastStatusCode(response.statusCode())
//         .build();
return null;

   };
   private static final String getBasicAuthenticationHeader(String username, String password) {
      String valueToEncode = username + ":" + password;
      return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
   }
   static class ProxyAuthenticator extends Authenticator {
      private final String username;
      private final String password;

      public ProxyAuthenticator(String username, String password) {
         this.username = username;
         this.password = password;
      }

      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
         return new PasswordAuthentication(username, password.toCharArray());
      }
   }
}
