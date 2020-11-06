package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.Arrays;
import java.util.List;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.CNOVANewCrawler;

public class SaopauloExtramarketplaceCrawler extends CNOVANewCrawler {

   private static final String STORE = "extra";
   private static final String INITIALS = "EX";
   private static final List<String> SELLER_NAMES = Arrays.asList("Extra", "extra.com.br");

   public SaopauloExtramarketplaceCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.FETCHER);
   }

   @Override
   protected Response fetchPage(String url, boolean tryAgain) {
      Request request = RequestBuilder.create()
            .setUrl(url)
            .build();

      return new ApacheDataFetcher().get(session, request);
   }

   //
   // @Override
   // protected Response fetchPage(String url, boolean tryAgain) {
   // Map<String, String> headers = new HashMap<>();
   // headers.put("referer", session.getOriginalURL());
   // headers.put("authority", "www.extra.com.br");
   // headers.put("cache-control", "max-age=0");
   // headers.put("upgrade-insecure-requests", "1");
   // headers.put("accept", "text/html");
   // headers.put("sec-fetch-site", "cross-site");
   // headers.put("sec-fetch-mode", "navigate");
   // headers.put("sec-fetch-user", "?1");
   // headers.put("sec-fetch-dest", "document");
   // headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6");
   //
   // Request request = RequestBuilder.create()
   // .setUrl(url)
   // .setCookies(cookies)
   // .setHeaders(headers)
   // .setFetcheroptions(FetcherOptionsBuilder.create()
   // .mustUseMovingAverage(false)
   // .mustRetrieveStatistics(true)
   // .build())
   // .mustSendContentEncoding(false)
   // .setProxyservice(
   // Arrays.asList(
   // ProxyCollection.INFATICA_RESIDENTIAL_BR,
   // ProxyCollection.NETNUT_RESIDENTIAL_BR,
   // ProxyCollection.BUY
   // )
   // ).build();
   //
   // Response response = this.dataFetcher.get(session, request);
   // this.cookies.addAll(response.getCookies());
   //
   // int statusCode = response.getLastStatusCode();
   //
   // if (tryAgain && (response.getBody().isEmpty() || (Integer.toString(statusCode).charAt(0) != '2'
   // &&
   // Integer.toString(statusCode).charAt(0) != '3'
   // && statusCode != 404))) {
   //
   // if (this.dataFetcher instanceof FetcherDataFetcher) {
   // response = new JavanetDataFetcher().get(session, request);
   // } else {
   // response = new FetcherDataFetcher().get(session, request);
   // }
   //
   // }
   //
   // return response;
   // }

   @Override
   protected String getStore() {
      return STORE;
   }

   @Override
   protected List<String> getSellerName() {
      return SELLER_NAMES;
   }

   @Override
   protected String getInitials() {
      return INITIALS;
   }
}
