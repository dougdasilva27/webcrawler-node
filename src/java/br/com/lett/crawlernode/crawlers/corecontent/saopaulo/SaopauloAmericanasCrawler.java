package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.Arrays;
import com.google.common.net.HttpHeaders;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.B2WCrawler;

public class SaopauloAmericanasCrawler extends B2WCrawler {

  private static final String HOME_PAGE = "https://www.americanas.com.br/";
  private static final String MAIN_SELLER_NAME_LOWER = "americanas.com";

  public SaopauloAmericanasCrawler(Session session) {
    super(session);
    super.config.setFetcher(FetchMode.JAVANET);
    super.subSellers = Arrays.asList("lojas americanas", "lojas americanas mg", "lojas americanas rj", "lojas americanas sp", "lojas americanas rs");
    super.sellerNameLower = MAIN_SELLER_NAME_LOWER;
    super.homePage = HOME_PAGE;
  }

  @Override
  protected void setHeaders() {
    headers.put(
        HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3"
    );
    headers.put(HttpHeaders.CACHE_CONTROL, "max-age=0");
    headers.put(HttpHeaders.CONNECTION, "keep-alive");
    headers.put(HttpHeaders.USER_AGENT, "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.100 Safari/537.36");
    headers.put(HttpHeaders.ACCEPT_LANGUAGE, "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6");
    headers.put(HttpHeaders.ACCEPT_ENCODING, "no");
    headers.put("Upgrade-Insecure-Requests", "1");
  }
}
