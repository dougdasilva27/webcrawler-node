package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;


import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;

import java.util.HashMap;
import java.util.Map;

public class BrasilExtrabomesCrawler extends BrasilExtrabomCrawler {

   private static final String API = "https://www.extrabom.com.br/carrinho/verificarCepDeposito/";
   private static final String CEP = "29.144-028";

   public BrasilExtrabomesCrawler(Session session) {

      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   @Override
   protected void processBeforeFetch() {
      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/x-www-form-urlencoded");
      String payload = "cep=" + CEP;

      Request request = Request.RequestBuilder.create()
         .setUrl(API)
         .setHeaders(headers)
         .setPayload(payload)
         .build();

      Response response = dataFetcher.post(session, request);
      this.cookies = response.getCookies();
   }
}
