package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class BrasilExtrabomesCrawler extends BrasilExtrabomCrawler {

   private static final String API = "https://www.extrabom.com.br/carrinho/verificarCepDeposito/";
   private static final String CEP = "29.144-028";

   public BrasilExtrabomesCrawler(Session session) {
      super(session);
      config.setFetcher(FetchMode.FETCHER);
   }

   @Override
   public void handleCookiesBeforeFetch() {
      Map<String, String> headers = new HashMap<>();
      headers.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.82 Safari/537.36");

      String payload = "cep=" + CEP;

      Request request = Request.RequestBuilder.create()
         .setUrl(API)
         .setHeaders(headers)
         .setProxyservice(
            Arrays.asList(
               ProxyCollection.NO_PROXY))
         .setPayload(payload)
         .build();

      Response response = dataFetcher.post(session, request);
      this.cookies = response.getCookies();
   }
}
