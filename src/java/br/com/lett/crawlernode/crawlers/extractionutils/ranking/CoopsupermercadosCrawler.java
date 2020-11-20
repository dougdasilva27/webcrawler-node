package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public abstract class CoopsupermercadosCrawler extends CrawlerRankingKeywords {

   private static String keyword;

   public CoopsupermercadosCrawler(Session session) {
      super(session);
   }

   protected abstract String getLocation();

   private Document fetchSearchPage(){

      String url = "https://www.cooplojaonline.com.br/" + this.keywordEncoded;

      Request request = Request.RequestBuilder.create().setUrl(url).setCookies(cookies).build();

      return Jsoup.parse(this.dataFetcher.get(session, request).getBody());
   }

   private String fetchKeywordCode(Document doc){

      String script = doc.select("#departament-navegador > script").first().html();

      return CrawlerUtils.extractSpecificStringFromScript(script, "fq=", false, "&", false);

   }


   private int fetchTotalProducts(Document doc){

      int totalProducts = 0;

      if(doc != null){
            totalProducts = CrawlerUtils.scrapIntegerFromHtml(doc, ".resultado-busca-numero .value", true, 0);
      }

      return totalProducts;
   }

   private void handleCookiesBeforeFetch() {

      String payload = "{\"public\":{\"country\":{\"value\":\"BRA\"},\"regionId\":{\"value\":\"" + getLocation() +"\"}}}";

      HashMap<String, String> headers = new HashMap<>();
      headers.put("Content-type", "application/json");

      Request request = Request.RequestBuilder.create().setUrl("https://www.cooplojaonline.com.br/api/sessions/").setHeaders(headers).setPayload(payload)
         .build();
      Response response = this.dataFetcher.post(session, request);

      for (Cookie cookieResponse : response.getCookies()) {
         BasicClientCookie cookie = new BasicClientCookie(cookieResponse.getName(), cookieResponse.getValue());
         cookie.setDomain("www.cooplojaonline.com.br");
         cookie.setPath("/");
         this.cookies.add(cookie);
      }
   }

   @Override
   protected void extractProductsFromCurrentPage(){
      this.pageSize = 12;
      Document searchPage = null;

      this.log("Página " + this.currentPage);

      if(this.currentPage == 1){
         handleCookiesBeforeFetch();
         searchPage = fetchSearchPage();

         keyword = fetchKeywordCode(searchPage);
      }

      String url = "https://www.cooplojaonline.com.br/buscapagina?fq=" + keyword
         + "&PS=12&sl=e508d2a0-708d-4b8b-bbbd-3bf956e9ab49&cc=12&sm=0&PageNumber=" + this.currentPage;

      Request request = Request.RequestBuilder.create().setUrl(url).setCookies(cookies).build();
      this.currentDoc = Jsoup.parse(this.dataFetcher.get(session, request).getBody());
      this.log("Link onde são feitos os crawlers: " + url);

      Elements products = this.currentDoc.select(".productLIst ul .productContent");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts(searchPage);
         }
         for (Element e : products) {
            String productUrl = CrawlerUtils.completeUrl(CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "a", "href"), "https", "www.comper.com.br");
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".insert-sku-checkbox", "rel");
            saveDataProduct(internalId, null, productUrl);
            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
            if (this.arrayProducts.size() == productsLimit)
               break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }
      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

   }


   private void setTotalProducts(Document doc) {
      this.totalProducts = fetchTotalProducts(doc);
      this.log("Total da busca: " + this.totalProducts);
   }
}
