package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.B2WCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import org.apache.http.cookie.Cookie;

import java.io.IOException;
import java.net.HttpCookie;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static br.com.lett.crawlernode.util.CrawlerUtils.setCookie;

public class SaopauloAmericanasCrawler extends B2WCrawler {

   private static final String HOME_PAGE = "https://www.americanas.com.br/";
   private static final String URL_PAGE_OFFERS = "https://www.americanas.com.br/parceiros/";
   private static final String MAIN_SELLER_NAME_LOWER = "americanas.com";
   private static final String MAIN_SELLER_NAME_LOWER_FROM_HTML = "Americanas";

   public SaopauloAmericanasCrawler(Session session) {
      super(session);
      super.subSellers = Arrays.asList("b2w", "lojas americanas", "lojas americanas mg", "lojas americanas rj", "lojas americanas sp", "lojas americanas rs", "Lojas Americanas", "americanas");
      super.sellerNameLower = MAIN_SELLER_NAME_LOWER;
      super.sellerNameLowerFromHTML = MAIN_SELLER_NAME_LOWER_FROM_HTML;
      super.homePage = HOME_PAGE;
      super.urlPageOffers = URL_PAGE_OFFERS;
      super.config.setFetcher(FetchMode.JSOUP);
   }

   @Override
   public void handleCookiesBeforeFetch() {

      try {
         String url_americanas = "https://www.americanas.com.br";
         HttpResponse<String> response = retryRequest(url_americanas, session);
         List<String> cookiesResponse = response.headers().map().get("Set-Cookie");
         for (String cookieStr : cookiesResponse) {
            HttpCookie cookie = HttpCookie.parse(cookieStr).get(0);
            cookies.add(setCookie(cookie.getName(), cookie.getValue(), CommonMethods.getLast(url_americanas.split("//")), "/"));
         }
      } catch (Exception e) {
         throw new RuntimeException("Failed In load document: " + session.getOriginalURL(), e);
      }
   }

   //      Request request = Request.RequestBuilder.create()
//         .setHeaders(B2WCrawler.getHeaders())
//         .setUrl(url_americanas)
//         .setProxyservice(List.of(
//            ProxyCollection.NETNUT_RESIDENTIAL_BR,
//            ProxyCollection.NETNUT_RESIDENTIAL_MX,
//            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
//            ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY
//         ))
//         .build();
   //    Response response = CrawlerUtils.retryRequest(request, session, new ApacheDataFetcher(), true);
//      List<Cookie> cookiesResponse = response.getCookies();
//      for (Cookie cookieResponse : cookiesResponse) {
//         cookies.add(setCookie(cookieResponse.getName(), cookieResponse.getValue(), CommonMethods.getLast(url_americanas.split("//")), "/"));
//      }
//   }
   public static HttpResponse retryRequest(String urlAmericanas, Session session) throws IOException, InterruptedException {
      HttpResponse<String> response = null;
      ArrayList<Integer> ipPort = new ArrayList<Integer>();
      ipPort.add(3132); //netnut br haproxy
      ipPort.add(3135); // buy haproxy
      ipPort.add(3133); //netnut ES haproxy
      ipPort.add(3138); //netnut AR haproxy
      ipPort.add(3137); //netnut CH haproxy

      try {
         for (int interable = 0; interable < ipPort.size(); interable++) {
            response = getRequest(urlAmericanas, ipPort.get(interable));
            if (response.statusCode() == 200) {
               return response;
            }
         }
      } catch (Exception e) {
         throw new RuntimeException("Failed In load document: " + session.getOriginalURL(), e);
      }
      return response;
   }

   private static HttpResponse getRequest(String urlAmericanas, Integer port) throws IOException, InterruptedException {
      Integer attempt = 0;
      HttpResponse<String> response;
      do {
         response = RequestHandler(urlAmericanas, port);
         attempt ++;
      } while (response.statusCode() != 200 && attempt < 3);
      return response;
   }

   private static HttpResponse RequestHandler(String urlAmericanas, Integer port) throws IOException, InterruptedException {
      HttpClient client = HttpClient.newBuilder().proxy(ProxySelector.of(new InetSocketAddress("haproxy.lett.global", port))).build();
      HttpRequest request = HttpRequest.newBuilder()
         .GET()
         .uri(URI.create(urlAmericanas))
         .build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      return response;
   }
}
