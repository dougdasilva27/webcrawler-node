package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import java.util.HashMap;
import java.util.Map;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.brasil.BrasilCarrefourCrawler;

/**
 * 18/04/2020
 * 
 * @author Fabrício
 *
 */
public class SaopauloCarrefourbrooklinCrawler extends BrasilCarrefourCrawler {

   public SaopauloCarrefourbrooklinCrawler(Session session) {
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
      headers.put("cookie", "statusCepConsultation=true; cepConsultation=04702-000; sideStoreOn=true; "
            + "selectedPointOfServices=BRA124%2CBRA084%2CBRA007%2CBRA001%2CBRA300%2CBRA302%2CBRA303%2CBRA306%2CBRA307%2CBRA500%2CBRA313%2CBRA315%2Cbra317%2CBRA311%2CBRA312%2CBRADS;");

      Request request = RequestBuilder.create().setUrl(url).setHeaders(headers).build();
      return this.dataFetcher.get(session, request).getBody();
   }
}
