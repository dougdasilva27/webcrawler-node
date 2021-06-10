package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArgentinaMillanCrawler extends CrawlerRankingKeywords {

   public ArgentinaMillanCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 12;
      this.log("Página " + this.currentPage);

      String keyword = this.keywordWithoutAccents.replaceAll(" ", "+");
      String url = "https://atomoconviene.com/atomo-ecommerce/index.php?controller=search&s=" + keyword + "&page=" + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select("div[id=js-product-list] div.products > article");

      if (products != null && !products.isEmpty()) {
         if (this.totalProducts == 0) setTotalProducts();

         for (Element e : products) {
            String internalPid = e.attr("data-id-product");
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "div.card-img-top.product__card-img > a", "href");

            saveDataProduct(null, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);
            if (this.arrayProducts.size() == productsLimit) break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {
      return this.currentDoc.selectFirst("a[rel=next]") != null;
   }

   @Override
   protected void setTotalProducts() {
      Element totalElement = this.currentDoc.select("div.visible--mobile.text-sm-center.mt-1.col-12 ").first();

      if (totalElement != null) {
         String[] splittedStr = totalElement.html().split(" ");

         if(splittedStr.length > 0){
            this.totalProducts = Integer.parseInt(splittedStr[3]);
         }

         this.log("Total da busca: " + this.totalProducts);
      }
   }
}
