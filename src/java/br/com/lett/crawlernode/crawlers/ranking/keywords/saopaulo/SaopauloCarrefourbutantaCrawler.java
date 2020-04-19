package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.HashMap;
import java.util.Map;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;

/**
 * 18/04/2020
 * 
 * @author Fabrício
 *
 */
public class SaopauloCarrefourbutantaCrawler extends BrasilCarrefourCrawler {

   public SaopauloCarrefourbutantaCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   @Override
   protected String fetchPage(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6");
      headers.put("sec-fetch-user", "?1");
      headers.put("sec-fetch-site", "none");
      headers.put("sec-fetch-mode", "navigate");
      headers.put("referer", "https://www.carrefour.com.br/");
      headers.put("cookie", "statusCepConsultation=true; cepConsultation=05512-300; sideStoreOn=true; "
            + "selectedPointOfServices=BRA057%2CBRA006%2CBRA005%2CBRA003%2CBRA085%2CBRA079%2CBRA050%2CBRA301%2CBRA305%2CBRA308%2CBRA501%2CBRA005;");

      Request request = RequestBuilder.create().setUrl(url).setHeaders(headers).build();
      return this.dataFetcher.get(session, request).getBody();
   }
}
