package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpHeaders;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public abstract class ComperCrawlerRanking extends CrawlerRankingKeywords {

   public ComperCrawlerRanking(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   protected final String storeId = getStoreId();
   protected final String storeUf = getStoreUf();
   private String userAgent;


   protected abstract String getStoreId();
   protected abstract String getStoreUf();

   private int fetchTotalProducts(){

      int totalProducts = 0;

      String url = "https://www.comper.com.br/" + this.keywordEncoded + "?utm_source=" + storeUf;
      BasicClientCookie cookie = new BasicClientCookie("VTEXSC", "sc="+getStoreId());
      cookie.setDomain("www.comper.com.br");
      cookie.setPath("/");
      this.cookies.add(cookie);
      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();

      Document response = Jsoup.parse(this.dataFetcher.get(session, request).getBody()) ;

      if(response != null){

         totalProducts = CrawlerUtils.scrapIntegerFromHtml(response, ".resultado-busca-numero .value", true, 0);
      }

      return totalProducts;
   }

   @Override
   public void extractProductsFromCurrentPage() {
      // número de produtos por página do market
      this.pageSize = 32;

      this.log("Página " + this.currentPage);

      String url = "https://www.comper.com.br/buscapagina?&ft=" + this.keywordEncoded + "&PS=32&sl=df48a27d-fc0a-47cd-8087-ac49751cd86b&cc=32&sm=0&PageNumber="
         + this.currentPage + "&O=OrderByScoreDESC&sc=" + getStoreId();

      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.USER_AGENT, this.userAgent);

      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
      this.currentDoc = Jsoup.parse(this.dataFetcher.get(session, request).getBody());
      this.log("Link onde são feitos os crawlers: " + url);

      Elements products = this.currentDoc.select("ul .shelf-item");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String productUrl = CrawlerUtils.completeUrl(CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "a", "href"), "https", "www.comper.com.br");
            String internalId = e.attr("data-product-id");

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
