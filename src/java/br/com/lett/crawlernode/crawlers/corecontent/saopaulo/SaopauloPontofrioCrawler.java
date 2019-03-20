package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.HashMap;
import java.util.Map;
import br.com.lett.crawlernode.core.fetcher.DataFetcherNO;
import br.com.lett.crawlernode.core.fetcher.methods.GETFetcher;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.CNOVACrawler;

public class SaopauloPontofrioCrawler extends CNOVACrawler {

  private static final String MAIN_SELLER_NAME_LOWER = "pontofrio";
  private static final String MAIN_SELLER_NAME_LOWER_2 = "pontofrio.com";
  private static final String HOST = "www.pontofrio.com.br";

  public SaopauloPontofrioCrawler(Session session) {
    super(session);
    super.mainSellerNameLower = MAIN_SELLER_NAME_LOWER;
    super.mainSellerNameLower2 = MAIN_SELLER_NAME_LOWER_2;
    super.marketHost = HOST;
  }

  @Override
  protected String fetchPage(String url) {
    Map<String, String> headers = new HashMap<>();
    headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
    headers.put("Accept-Enconding", "");
    headers.put("Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");
    headers.put("Cache-Control", "no-cache");
    headers.put("Connection", "keep-alive");
    headers.put("Host", HOST);
    headers.put("Referer", PROTOCOL + "://" + HOST + "/");
    headers.put("Upgrade-Insecure-Requests", "1");
    headers.put("User-Agent", DataFetcherNO.randUserAgent());

    return GETFetcher.fetchPageGETWithHeaders(session, url, cookies, headers, 1);
  }
}
