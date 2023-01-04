package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Parser;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class BrasilNestleAteVoceCrawler extends Crawler {

   public BrasilNestleAteVoceCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.FETCHER);
      super.config.setParser(Parser.JSON);
   }

   private final String PASSWORD = getPassword();
   private final String LOGIN = getLogin();

   protected String getLogin() {
      return session.getOptions().optString("login");
   }

   protected String getPassword() {
      return session.getOptions().optString("password");
   }

   @Override
   protected Response fetchResponse() {

      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/json");
      headers.put("x-authorization", "");
//      headers.put("origin", "https://www.nestleatevoce.com.br");
      headers.put("referer", "https://www.nestleatevoce.com.br/login");
//      headers.put("authority", "www.nestleatevoce.com.br");
//      headers.put("Connection", "keep-alive");
//      headers.put("Accept-Encoding", "gzip, deflate, br");

      String payload = "{\"operationName\":\"signIn\",\"variables\":{\"taxvat\":\"" + LOGIN + "\",\"password\":\"" + PASSWORD + "\"},\"query\":\"mutation signIn($taxvat: String!, $password: String!) {\\ngenerateCustomerToken(taxvat: $taxvat, password: $password) {\\ntoken\\nis_clube_nestle\\nenabled_club_nestle\\n__typename\\n}\\n}\\n\"}";


      Request request = Request.RequestBuilder.create()
         .setUrl("https://www.nestleatevoce.com.br/graphql")
         .setHeaders(headers)
         .setPayload(payload)
         .setProxyservice(Arrays.asList(ProxyCollection.BUY))
         .build();

//      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new JsoupDataFetcher(), new ApacheDataFetcher(), new FetcherDataFetcher()), session);
      Response response = new FetcherDataFetcher().post(session, request);

      return response;
   }
}
