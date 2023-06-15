package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.B2WCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.net.HttpHeaders;
import org.apache.http.cookie.Cookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.HttpCookie;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

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

   public static Map<String, String> getHeadersAmericanas() {
      Random random = new Random();

      Map<String, String> headers = new HashMap<>();

      headers.put("user-agent", UserAgent.get(random.nextInt(UserAgent.size())));
      headers.put(HttpHeaders.REFERER, homePage);
      headers.put(
         HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"
      );
      headers.put(HttpHeaders.CACHE_CONTROL, "max-age=0");
      headers.put(HttpHeaders.ACCEPT_LANGUAGE, "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6");
      headers.put("sec-fetch-site", "none");
      headers.put("sec-fetch-mode", "navigate");
      headers.put("sec-fetch-user", "?1");
      headers.put("sec-fetch-dest", "document");

      return headers;
   }

   @Override
   protected Document fetch() {
      return Jsoup.parse(fetchPage(session.getOriginalURL(), this.dataFetcher, cookies, headers, session));
   }


   @Override
   public String fetchPage(String url, DataFetcher df, List<Cookie> cookies, Map<String, String> headers, Session session) {

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .setHeaders(headers)
         .setSendUserAgent(false)
         .setFetcheroptions(
            FetcherOptions.FetcherOptionsBuilder.create()
               .mustUseMovingAverage(false)
               .mustRetrieveStatistics(true)
               .setForbiddenCssSelector("#px-captcha")
               .build()
         )
         .setProxyservice(
            Arrays.asList(

               ProxyCollection.NETNUT_RESIDENTIAL_ROTATE_BR,
               ProxyCollection.NETNUT_RESIDENTIAL_ROTATE_AR,
               ProxyCollection.NETNUT_RESIDENTIAL_ROTATE_MX

            )
         )
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new FetcherDataFetcher(), new ApacheDataFetcher(), new JsoupDataFetcher(), new FetcherDataFetcher()), session);

      return response.getBody();
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
         attempt++;
      } while (response.statusCode() != 200 && attempt < 3);
      return response;
   }

   private static HttpResponse RequestHandler(String urlAmericanas, Integer port) throws IOException, InterruptedException {
      String[] listaHeaders = new String[18];
      Map<String, String> headers = getHeadersAmericanas();
      int i = 0;
      for (String chave : headers.keySet()) {
         listaHeaders[i++] = chave;
         listaHeaders[i++] = headers.get(chave);
      }
      HttpClient client = HttpClient.newBuilder().proxy(ProxySelector.of(new InetSocketAddress("haproxy.lett.global", port))).build();
      HttpRequest request = HttpRequest.newBuilder()
         .GET()
         .headers(listaHeaders)
         .uri(URI.create(urlAmericanas))
         .build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      return response;
   }
}
