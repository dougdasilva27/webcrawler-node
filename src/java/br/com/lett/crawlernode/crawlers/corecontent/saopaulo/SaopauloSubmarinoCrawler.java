package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions.FetcherOptionsBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.B2WCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
      super.config.setFetcher(FetchMode.JSOUP);

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

   public static String fetchPage(String url, Session session) {
      Map<String, String> headers = new HashMap<>();
      headers.put("host", "www.shoptime.com.br");
      headers.put("sec-ch-ua", " \" Not A;Brand\";v=\"99\", \"Chromium\";v=\"90\", \"Google Chrome\";v=\"90\"");
      headers.put("sec-ch-ua-mobile", "?0");
      headers.put("upgrade-insecure-requests", "1");
      headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,/;q=0.8,application/signed-exchange;v=b3;q=0.9");
      headers.put("sec-fetch-site", "none");
      headers.put("sec-fetch-mode", "navigate");
      headers.put("sec-fetch-user", "?1");
      headers.put("sec-fetch-dest", "document");
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6");
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .mustSendContentEncoding(false)
         .setHeaders(headers)
         .setFetcheroptions(
            FetcherOptions.FetcherOptionsBuilder.create()
               .mustUseMovingAverage(false)
               .mustRetrieveStatistics(true)
               .setForbiddenCssSelector("#px-captcha")
               .build()
         ).setProxyservice(
            Arrays.asList(
               ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY
            )
         ).build();


      Response response = new ApacheDataFetcher().get(session, request);
      String content = response.getBody();

      int statusCode = response.getLastStatusCode();

      if ((Integer.toString(statusCode).charAt(0) != '2' &&
         Integer.toString(statusCode).charAt(0) != '3'
         && statusCode != 404)) {
         request.setProxyServices(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_DE_HAPROXY
         ));

         content = new JsoupDataFetcher().get(session, request).getBody();
      }

      return content;
   }

   @Override
   protected Document fetch() {
      setHeaders();

      return Jsoup.parse(fetchPage(session.getOriginalURL(), session));

   }

}
