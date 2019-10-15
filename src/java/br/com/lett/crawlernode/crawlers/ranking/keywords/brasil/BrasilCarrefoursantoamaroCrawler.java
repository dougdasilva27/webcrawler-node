package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.HashMap;
import java.util.Map;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;

public class BrasilCarrefoursantoamaroCrawler extends BrasilCarrefourCrawler {

  public BrasilCarrefoursantoamaroCrawler(Session session) {
    super(session);
    super.fetchMode = FetchMode.FETCHER;
  }

  @Override
  protected String fetchPage(String url) {
    Map<String, String> headers = new HashMap<>();
    headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
    headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6");
    headers.put("upgrade-insecure-requests", "1");
    headers.put("referer", "https://www.carrefour.com.br/");
    headers.put("cookie", "statusCepConsultation=true; cepConsultation=04555-002; sideStoreOn=true; "
        + "selectedPointOfServices=BRA124%2CBRA084%2CBRA007%2CBRA001%2CBRADS%2CBRA300%2CBRA302%2CBRA303%2CBRA306%2CBRA307%2CBRA500;");

    Request request = RequestBuilder.create().setUrl(url).setHeaders(headers).build();
    return this.dataFetcher.get(session, request).getBody();
  }
}
