package br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class MercadolivreCrawler extends CrawlerRankingKeywords {

   private String nextUrlHost;
   private String nextUrl;
   private String productUrlHost;
   private String url;

   private static final String PRODUCTS_SELECTOR = ".results-item .item";
   protected Integer meliPageSize = 64;

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

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = meliPageSize;
      this.log("Página " + this.currentPage);

      // In this market we need to scrap url for next page
      // because the url format change
      if (this.currentPage > 1) {
         this.url = this.nextUrl;
      }

      this.currentDoc = fetchDocument(this.url);
      this.nextUrl = CrawlerUtils.scrapUrl(currentDoc, ".andes-pagination__button--next > a", "href", "https:", nextUrlHost);
      Elements products = this.currentDoc.select(PRODUCTS_SELECTOR);
      boolean ownStoreResults = !this.currentDoc.select("#categorySearch").isEmpty();
      if (!products.isEmpty() && ownStoreResults) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String internalPid = e.id();
            String productUrl = CrawlerUtils.scrapUrl(e, "> a", "href", "https:", productUrlHost);
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

   @Override
   protected boolean hasNextPage() {
      return super.hasNextPage() && this.nextUrl != null;
   }

   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(currentDoc, ".quantity-results", true, 0);
      this.log("Total da busca: " + this.totalProducts);
   }

}
