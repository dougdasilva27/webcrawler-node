package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.Arrays;
import com.google.common.net.HttpHeaders;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions.FetcherOptionsBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.B2WCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class SaopauloAmericanasCrawler extends B2WCrawler {

  private static final String HOME_PAGE = "https://www.americanas.com.br/";
  private static final String MAIN_SELLER_NAME_LOWER = "americanas.com";

  public SaopauloAmericanasCrawler(Session session) {
    super(session);
    super.subSellers = Arrays.asList("lojas americanas", "lojas americanas mg", "lojas americanas rj", "lojas americanas sp", "lojas americanas rs");
    super.sellerNameLower = MAIN_SELLER_NAME_LOWER;
    super.homePage = HOME_PAGE;
  }

  @Override
  protected void setHeaders() {
    headers.put(HttpHeaders.REFERER, this.homePage);
    headers.put(
        HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3"
    );
    headers.put(HttpHeaders.CACHE_CONTROL, "max-age=0");
    headers.put(HttpHeaders.CONNECTION, "keep-alive");
    headers.put(HttpHeaders.ACCEPT_LANGUAGE, "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6");
    headers.put(HttpHeaders.ACCEPT_ENCODING, "");
    headers.put("Upgrade-Insecure-Requests", "1");
    headers.put("sec-fetch-mode", "navigate");
    headers.put("sec-fetch-user", "?1");
    headers.put("sec-fetch-site", "none");
  }

  @Override
  public void handleCookiesBeforeFetch() {
    Request request;

    if (dataFetcher instanceof FetcherDataFetcher) {
      request = RequestBuilder.create().setUrl(HOME_PAGE)
          .setCookies(cookies)
          .setProxyservice(
              Arrays.asList(
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

    this.cookies = CrawlerUtils.fetchCookiesFromAPage(request, "www.americanas.com.br", "/", null, session, dataFetcher);
  }
}
