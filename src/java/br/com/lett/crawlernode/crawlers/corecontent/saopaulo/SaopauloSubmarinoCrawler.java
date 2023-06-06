package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.B2WCrawler;
import br.com.lett.crawlernode.util.CommonMethods;

import java.net.HttpCookie;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import static br.com.lett.crawlernode.util.CrawlerUtils.setCookie;

public class SaopauloSubmarinoCrawler extends B2WCrawler {

   private static final String HOME_PAGE = "https://www.submarino.com.br/";
   private static final String URL_PAGE_OFFERS = "https://www.submarino.com.br/parceiros/";
   private static final String MAIN_SELLER_NAME_LOWER = "submarino";
   private static final String MAIN_SELLER_NAME_LOWER_FROM_HTML = "Submarino";


   public SaopauloSubmarinoCrawler(Session session) {
      super(session);
      super.subSellers = new ArrayList<>();
      super.sellerNameLower = MAIN_SELLER_NAME_LOWER;
      super.sellerNameLowerFromHTML = MAIN_SELLER_NAME_LOWER_FROM_HTML;
      super.homePage = HOME_PAGE;
      super.urlPageOffers = URL_PAGE_OFFERS;
   }

   @Override
   public void handleCookiesBeforeFetch() {

      try {
         HttpClient client = HttpClient.newBuilder().proxy(ProxySelector.of(new InetSocketAddress("haproxy.lett.global", 3130))).build();
         HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(homePage))
            .build();
         HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

         List<String> cookiesResponse = response.headers().map().get("Set-Cookie");
         for (String cookieStr : cookiesResponse) {
            HttpCookie cookie = HttpCookie.parse(cookieStr).get(0);
            cookies.add(setCookie(cookie.getName(), cookie.getValue(), CommonMethods.getLast(homePage.split("//")), "/"));
         }
      } catch (Exception e) {
         throw new RuntimeException("Failed In load document: " + session.getOriginalURL(), e);
      }
   }
}
