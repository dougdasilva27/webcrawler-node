package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions.FetcherOptionsBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.B2WCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;

import java.util.ArrayList;
import java.util.Arrays;

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
      Request request;
      if (dataFetcher instanceof FetcherDataFetcher) {
         request = RequestBuilder.create().setUrl(HOME_PAGE)
            .setCookies(cookies)
            .setProxyservice(
               Arrays.asList(
                  ProxyCollection.NETNUT_RESIDENTIAL_BR,
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

      this.cookies = CrawlerUtils.fetchCookiesFromAPage(request, "www.submarino.com.br", "/", null, session, dataFetcher);
   }


}
