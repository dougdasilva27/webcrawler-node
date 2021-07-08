package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class GpsfarmaCrawler extends CrawlerRankingKeywords {

   public GpsfarmaCrawler(Session session) {
      super(session);
   }

   private String categoryUrl;

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 10;
      this.log("Página " + this.currentPage);

      String url = "https://gpsfarma.com/index.php/catalogsearch/result/index/?p=" + this.currentPage + "&q=" + this.keywordEncoded;

      if (this.currentPage > 1 && this.categoryUrl != null) {
         url = this.categoryUrl + "?p=" + this.currentPage;
      }

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select("ol.products.list.items > li");

      if (this.currentPage == 1) {
         String redirectUrl = CrawlerUtils.getRedirectedUrl(url, session);

         if (!url.equals(redirectUrl)) {
            this.categoryUrl = redirectUrl;
         }
      }

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }
         for (Element e : products) {

            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "div.price-box.price-final_price", "data-product-id");
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "form[data-role=tocart-form]", "data-product-sku");
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "a.product-item-link", "href");

            saveDataProduct(internalId, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
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
      Element totalSearchElement = this.currentDoc.selectFirst("p.toolbar-amount :last-child");

      if(totalSearchElement != null) {
         this.totalProducts = Integer.parseInt(totalSearchElement.text());
      }

      this.log("Total da busca: " + this.totalProducts);
   }
}
