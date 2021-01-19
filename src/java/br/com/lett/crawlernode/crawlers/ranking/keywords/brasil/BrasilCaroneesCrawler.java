package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.HashMap;
import java.util.Map;

public class BrasilCaroneesCrawler extends BrasilCaroneCrawler{
   private static final String CEP = "29056-255";
   private static final String HOME_PAGE = "https://www.carone.com.br/";
   private static final String API_LINK = "https://www.carone.com.br/carone/index/ajaxCheckPostcode/";

   public BrasilCaroneesCrawler(Session session) {
      super(session);
   }

   @Override
   public void processBeforeFetch() {
      Request request = Request.RequestBuilder.create()
         .setUrl(HOME_PAGE)
         .build();
      Response response = dataFetcher.get(session, request);
      Document document = Jsoup.parse(response.getBody());
      String key = CrawlerUtils.scrapStringSimpleInfoByAttribute(document, "input[name=form_key]", "value");

      String payload = "form_key=" + key + "&postcode=" + CEP;

      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/x-www-form-urlencoded; charset=UTF-8");

      Request requestApi = Request.RequestBuilder.create()
         .setUrl(API_LINK)
         .setPayload(payload)
         .setHeaders(headers)
         .build();

      Response responseApi = dataFetcher.post(session, requestApi);
      this.cookies.addAll(responseApi.getCookies());
   }
}
