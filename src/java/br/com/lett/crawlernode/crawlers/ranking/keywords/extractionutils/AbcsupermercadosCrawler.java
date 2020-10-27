package br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public abstract class AbcsupermercadosCrawler extends CrawlerRankingKeywords {

   protected final String storeId = getStoreId();
   private String userAgent;

   protected abstract String getStoreId();

   public AbcsupermercadosCrawler(Session session) {
      super(session);
   }

   private int fetchTotalProducts(){

      int totalProducts = 0;

      String url = "https://www.superabc.com.br/leite";
      BasicClientCookie cookie = new BasicClientCookie("VTEXSC", "sc="+getStoreId());
      cookie.setDomain("www.superabc.com.br");
      cookie.setPath("/");
      this.cookies.add(cookie);

      Request request = Request.RequestBuilder.create().setUrl(url).setCookies(cookies).build();

      Document response = Jsoup.parse(this.dataFetcher.get(session, request).getBody());

      if(response != null){

         totalProducts = CrawlerUtils.scrapIntegerFromHtml(response, ".resultado-busca-numero .value", true, 0);
      }

      return totalProducts;

   }


   @Override
   protected void extractProductsFromCurrentPage(){

      this.pageSize = 20;

      this.log("Página " + this.currentPage);

      String url = "https://www.superabc.com.br/buscapagina?ft=" + this.keywordEncoded + "&PS=20&sl=621a9e06-6be4-49e2-b3db-d87b97604e90&cc=20&sm=0&PageNumber="
         + this.currentPage;

      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.USER_AGENT, this.userAgent);

      Request request = Request.RequestBuilder.create().setUrl(url).setCookies(cookies).build();
      this.currentDoc = Jsoup.parse(this.dataFetcher.get(session, request).getBody());
      this.log("Link onde são feitos os crawlers: " + url);

      Elements products = this.currentDoc.select(".prateleira__item");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }
         for (Element e : products) {
            String productUrl = CrawlerUtils.completeUrl(CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "a", "href"), "https", "www.comper.com.br");
            String internalId = e.attr("data-id");
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

   @Override
   protected void setTotalProducts() {
      this.totalProducts = fetchTotalProducts();
      this.log("Total da busca: " + this.totalProducts);
   }
}
