package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

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
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.cookie.Cookie;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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

   public static String fetchPage(String url, DataFetcher df, List<Cookie> cookies, Map<String, String> headers, Session session) {

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
               ProxyCollection.NETNUT_RESIDENTIAL_BR,
               ProxyCollection.SMART_PROXY_BR,
               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_MX,
               ProxyCollection.SMART_PROXY_MX,
               ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_DE_HAPROXY,
               ProxyCollection.SMART_PROXY_CL,
               ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY,
               ProxyCollection.SMART_PROXY_MX_HAPROXY,
               ProxyCollection.SMART_PROXY_AR_HAPROXY,
               ProxyCollection.SMART_PROXY_PE_HAPROXY
            )
         )
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher(), new FetcherDataFetcher()), session);

      return response.getBody();
   }

}
