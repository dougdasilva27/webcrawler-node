package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.B2WCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.cookie.Cookie;

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
      String url_americanas = "https://www.americanas.com.br";
      Request request = Request.RequestBuilder.create()
         .setHeaders(B2WCrawler.getHeaders())
         .setUrl(url_americanas)
         .setProxyservice(List.of(
            ProxyCollection.SMART_PROXY_MX_HAPROXY,
            ProxyCollection.SMART_PROXY_AR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_MX,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY
         ))
         .build();
      Response response = CrawlerUtils.retryRequest(request, session, new ApacheDataFetcher(), true);
      List<Cookie> cookiesResponse = response.getCookies();
      for (Cookie cookieResponse : cookiesResponse) {
         cookies.add(setCookie(cookieResponse.getName(), cookieResponse.getValue(), CommonMethods.getLast(url_americanas.split("//")), "/"));
      }
   }
}
