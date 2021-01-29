package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import java.util.HashMap;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.google.common.net.HttpHeaders;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class MercadolivreCrawler extends CrawlerRankingKeywords {

   private String nextUrlHost;
   protected String nextUrl;
   private String productUrlHost;
   protected String url;

   protected MercadolivreCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   public void setNextUrlHost(String nextUrlHost) {
      this.nextUrlHost = nextUrlHost;
   }

   public void setUrl(String url) {
      this.url = url;
   }

   public void setProductUrlHost(String productUrlHost) {
      this.productUrlHost = productUrlHost;
   }

   private static final String PRODUCTS_SELECTOR = ".ui-search-layout__item div.andes-card";
   protected Integer meliPageSize = 64;

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = meliPageSize;
      this.log("Página " + this.currentPage);

      String searchUrl = getNextPageUrl();

      this.currentDoc = fetch(searchUrl);
      this.nextUrl = CrawlerUtils.scrapUrl(currentDoc, ".andes-pagination__button--next > a", "href", "https:", nextUrlHost);
      Elements products = this.currentDoc.select(PRODUCTS_SELECTOR);

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String productUrl = CrawlerUtils.scrapUrl(e, "a.ui-search-result__content, .ui-search-item__group--title a[title]", "href", "https", productUrlHost);

            String internalPid = null;
            if (productUrl != null) {
               if (productUrl.startsWith("https://www.mercadolivre.com.br/")) {
                  productUrl = productUrl != null ? productUrl.split("\\?")[0] : null;
                  internalPid = CommonMethods.getLast(productUrl.split("/"));
               } else {
                  internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "input[name=itemId]", "value");
               }
            }


            saveDataProduct(null, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);
            if (this.arrayProducts.size() == productsLimit)
               break;

         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private Document fetch(String url) {
      // This user agent is used because some of ours user agents doesn't work on this market
      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.116 Safari/537.36");

      Request request = RequestBuilder.create()
            .setCookies(cookies)
            .setUrl(url)
            .setHeaders(headers)
            .setFollowRedirects(false)
            .build();

      Response response = dataFetcher.get(session, request);

      return Jsoup.parse(response.getBody());
   }

   protected String getNextPageUrl() {
      return this.currentPage > 1 ? this.nextUrl : this.url;
   }

   @Override
   protected boolean hasNextPage() {
      return super.hasNextPage() && this.nextUrl != null;
   }

   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(currentDoc, ".ui-search-search-result__quantity-results", true, 0);
      this.log("Total da busca: " + this.totalProducts);
   }

}
