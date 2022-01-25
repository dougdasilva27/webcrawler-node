package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.VtexRankingKeywordsNew;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;

public class BrasilToymaniaCrawler extends VtexRankingKeywordsNew {
   public BrasilToymaniaCrawler(Session session) {
      super(session);
   }

   @Override
   protected JSONArray fetchPage(String url) {
      Request request = Request.RequestBuilder.create()
         .setUrl(url + "&O=OrderByReleaseDateDESC")
         .build();

      Response response = dataFetcher.get(session, request);

      return CrawlerUtils.stringToJsonArray(response.getBody());
   }

   @Override
   protected String setHomePage() {
      return "https://www.toymania.com.br";
   }
}
