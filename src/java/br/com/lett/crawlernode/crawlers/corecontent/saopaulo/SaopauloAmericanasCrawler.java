package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.Arrays;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions.FetcherOptionsBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.B2WCrawler;

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
  public String fetchPage(String url, Session session) {
    Request request = RequestBuilder.create().setUrl(url).setCookies(this.cookies).setHeaders(this.headers).mustSendContentEncoding(false)
        .setFetcheroptions(FetcherOptionsBuilder.create().mustUseMovingAverage(false).setForbiddenCssSelector("#px-captcha").build())
        .setProxyservice(Arrays.asList(ProxyCollection.INFATICA_RESIDENTIAL_BR, ProxyCollection.STORM_RESIDENTIAL_EU,
            ProxyCollection.BUY)).build();

    String content = this.dataFetcher.get(session, request).getBody();

    if (content == null || content.isEmpty()) {
      content = new ApacheDataFetcher().get(session, request).getBody();
    }

    return content;
  }
}
