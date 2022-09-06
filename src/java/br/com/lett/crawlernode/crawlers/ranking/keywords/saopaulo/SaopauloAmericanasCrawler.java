package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.B2WScriptPageCrawlerRanking;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SaopauloAmericanasCrawler extends B2WScriptPageCrawlerRanking {

   private static final String HOME_PAGE = "https://www.americanas.com.br/";

   public SaopauloAmericanasCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }


//   protected Document fetchPage() {
//      String keyword = this.keywordWithoutAccents.replace(" ", "-");
//
//      String url = homePage + "busca/" + keyword + "?limit=24&offset=" + (this.currentPage - 1) * pageSize;
//      this.log("Link onde são feitos os crawlers: " + url);
//
//      Map<String, String> headers = new HashMap<>();
//
//      headers.put("authority", "www.americanas.com.br");
//      headers.put("sec-ch-ua", " \" Not A;Brand\";v=\"99\", \"Chromium\";v=\"90\", \"Google Chrome\";v=\"90\"");
//      headers.put("sec-ch-ua-mobile", "?0");
//      headers.put("upgrade-insecure-requests", "1");
//      headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,/;q=0.8,application/signed-exchange;v=b3;q=0.9");
//      headers.put("sec-fetch-site", "none");
//      headers.put("sec-fetch-mode", "navigate");
//      headers.put("sec-fetch-user", "?1");
//      headers.put("sec-fetch-dest", "document");
//      headers.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.212 Safari/537.36");
//      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6");
//
//      Request request = Request.RequestBuilder.create()
//         .setUrl(url)
//         .setHeaders(headers)
//         .setSendUserAgent(false)
//         .setFetcheroptions(
//            FetcherOptions.FetcherOptionsBuilder.create()
//               .mustUseMovingAverage(false)
//               .mustRetrieveStatistics(true)
//               .setForbiddenCssSelector("#px-captcha")
//               .build()
//         )
//         .setProxyservice(
//            Arrays.asList(
//               ProxyCollection.NETNUT_RESIDENTIAL_BR,
//               ProxyCollection.NETNUT_RESIDENTIAL_MX,
//               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
//               ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
//               ProxyCollection.NETNUT_RESIDENTIAL_DE_HAPROXY,
//               ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY
//            )
//         )
//         .build();
//
//      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher(), new FetcherDataFetcher()), session);
//
//      return Jsoup.parse(response.getBody());
//   }


}
