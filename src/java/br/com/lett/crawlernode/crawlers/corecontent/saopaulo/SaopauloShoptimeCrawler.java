package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.ArrayList;
import java.util.Arrays;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions.FetcherOptionsBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.B2WCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class SaopauloShoptimeCrawler extends B2WCrawler {

   private static final String HOME_PAGE = "https://www.shoptime.com.br/";
   private static final String MAIN_SELLER_NAME_LOWER = "shoptime";

   public SaopauloShoptimeCrawler(Session session) {
      super(session);
      super.subSellers = new ArrayList<>();
      super.sellerNameLower = MAIN_SELLER_NAME_LOWER;
      super.homePage = HOME_PAGE;
   }

   @Override
   public void handleCookiesBeforeFetch() {
      Request request;

      if (dataFetcher instanceof FetcherDataFetcher) {
         request = RequestBuilder.create().setUrl(HOME_PAGE)
               .setCookies(cookies)
               .setProxyservice(
                     Arrays.asList(
                           ProxyCollection.INFATICA_RESIDENTIAL_BR,
                           ProxyCollection.STORM_RESIDENTIAL_EU,
                           ProxyCollection.STORM_RESIDENTIAL_US,
                           ProxyCollection.BUY
                     )
               ).mustSendContentEncoding(false)
               .setFetcheroptions(FetcherOptionsBuilder.create()
                     .setForbiddenCssSelector("#px-captcha")
                     .mustUseMovingAverage(false)
                     .mustRetrieveStatistics(true).build())
               .build();
      } else {
         request = RequestBuilder.create().setUrl(HOME_PAGE).setCookies(cookies).build();
      }

      this.cookies = CrawlerUtils.fetchCookiesFromAPage(request, "www.shoptime.com.br", "/", null, session, dataFetcher);
   }
}
