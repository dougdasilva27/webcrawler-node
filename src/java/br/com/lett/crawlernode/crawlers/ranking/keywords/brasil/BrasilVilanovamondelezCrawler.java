package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpHeaders;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;

public class BrasilVilanovamondelezCrawler extends CrawlerRankingKeywords {

   public BrasilVilanovamondelezCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   private static final String LOGIN_URL = "https://secure.vilanova.com.br/ckout/api/v2/customer/login";
   private static final String CNPJ = "33033028004090";
   private static final String PASSWORD = "681543";

   @Override
   protected void processBeforeFetch() {
      JSONObject payload = new JSONObject();
      payload.put("login", CNPJ);
      payload.put("password", PASSWORD);

      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded; charset=UTF-8");
      headers.put("sec-fetch-mode", "cors");
      headers.put("origin", "https://secure.vilanova.com.br");
      headers.put("sec-fetch-site", "same-origin");
      headers.put("x-requested-with", "XMLHttpRequest");

      String payloadString = "jsonData=";

      try {
         payloadString += URLEncoder.encode(payload.toString(), "UTF-8");
      } catch (UnsupportedEncodingException e) {
         Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
      }


      Request request = RequestBuilder.create().setUrl(LOGIN_URL).setPayload(payloadString).setHeaders(headers).build();
      List<Cookie> cookiesResponse = this.dataFetcher.post(session, request).getCookies();

      for (Cookie cookieResponse : cookiesResponse) {
         BasicClientCookie cookie = new BasicClientCookie(cookieResponse.getName(), cookieResponse.getValue());
         cookie.setDomain(".vilanova.com.br");
         cookie.setPath("/");
         this.cookies.add(cookie);
      }
   }


   @Override
   protected void extractProductsFromCurrentPage() {
      this.log("Página " + this.currentPage);

      this.pageSize = 24;
      String url = "https://www.vilanova.com.br/Busca/Resultado/?p=" + this.currentPage + "&loja=&q=" + this.keywordEncoded
            + "&ordenacao=6&limit=24";

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url, this.cookies);

      Elements products = this.currentDoc.select(".shelf-content-items .box-produto");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            Element internalIdElement = e.selectFirst(".text-center a");
            String internalId = internalIdElement.attr("data-codigoproduto");
            String productUrl = CrawlerUtils.scrapUrl(e, ".img-name a", "href", "https", "www.vilanova.com.br");

            saveDataProduct(internalId, null, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
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

   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".qtd-produtos", true, 0);
      this.log("Total da busca: " + this.totalProducts);
   }
}
