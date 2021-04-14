package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.cookie.Cookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class SupermarketCrawler extends CrawlerRankingKeywords {

   private static final String BASE_URL = "https://app.bigdatawifi.com.br/";
   private final String store = getStore();
   private String token;

   protected SupermarketCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   public String getStore() {
      return store;
   }

   public String fetchCookiesFromAPage() {
      List<Cookie> cookies = new ArrayList<>();
      String url = BASE_URL + store + "/ofertas";

      Request request = Request.RequestBuilder.create().setCookies(cookies).setUrl(url).setHeaders(null).build();
      Response response = this.dataFetcher.get(session, request);

      List<Cookie> cookiesResponse = response.getCookies();
      StringBuilder stringBuilder = new StringBuilder();
      for (Cookie cookieResponse : cookiesResponse) {
         stringBuilder.append(cookieResponse.getName());
         stringBuilder.append("=");
         stringBuilder.append(cookieResponse.getValue());
         stringBuilder.append("; ");
      }

      Document html = Jsoup.parse(response.getBody());
      token = CrawlerUtils.scrapStringSimpleInfoByAttribute(html, "meta[name='csrf-token']", "content");

      return stringBuilder.toString();
   }


   public Document crawlApi() {

      this.pageSize = 20;
      String apiUrl = BASE_URL + store + "/ofertas/buscar/buscar-produtos/" + (this.currentPage - 1) * pageSize;

      String payload = "busca=" + keywordEncoded;
      String hearderCookie = fetchCookiesFromAPage();

      Map<String, String> headers = new HashMap<>();
      headers.put("X-CSRF-TOKEN", token);
      headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
      headers.put("X-Requested-With", "XMLHttpRequest");
      headers.put("Origin", BASE_URL);
      headers.put("sec-ch-ua-mobile", "?0");
      headers.put("Accept", "*/*");
      headers.put("Connection", "keep-alive");
      headers.put("Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");
      headers.put("Cookie", hearderCookie);

      Request request = Request.RequestBuilder.create()
         .setUrl(apiUrl)
         .setPayload(payload)
         .setHeaders(headers)
         .build();
      String content = this.dataFetcher
         .post(session, request)
         .getBody();

      return Jsoup.parse(content);

   }

   @Override
   protected void extractProductsFromCurrentPage() {

      this.log("Página " + this.currentPage);

      this.currentDoc = crawlApi();

      //using pattern becausa dosn't work with jsoup
      List<String> products = pattern();

      if (!products.isEmpty()) {

         for (String product : products) {

            String internalId = product.replace("\\&quot;", "");
            String urlProduct = BASE_URL + store + "/ofertas/produto/detalhe/" + internalId;

            saveDataProduct(internalId, null, urlProduct);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + urlProduct);
            if (this.arrayProducts.size() == productsLimit) {
               break;

            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   public List<String> pattern() {
      List<String> lisIds = new ArrayList<>();
      Document doc = this.currentDoc;
      Pattern pattern = Pattern.compile("data-id=\\\"(.+?)\\\\n");
      Matcher matcher = pattern.matcher(doc.html());
      while (matcher.find()) {
         lisIds.add(matcher.group(1));
      }

      return lisIds;
   }

   @Override
   protected boolean hasNextPage() {
      return !this.currentDoc.toString().isEmpty();
   }

}
