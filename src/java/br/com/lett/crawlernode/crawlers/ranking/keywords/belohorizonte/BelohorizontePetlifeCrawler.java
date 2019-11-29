package br.com.lett.crawlernode.crawlers.ranking.keywords.belohorizonte;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BelohorizontePetlifeCrawler extends CrawlerRankingKeywords {

   public BelohorizontePetlifeCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 24;

      this.log("Página " + this.currentPage);

      String url = "https://www.petlifebh.com.br/page/" + this.currentPage + "/?s=" + this.keywordEncoded + "&product_cat=0&post_type=product";
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".products li");
      if (products.size() >= 1) {

         for (Element e : products) {
            String productUrl = CrawlerUtils.scrapUrl(e, "a.product-loop-title", "href", "https", "petlifebh");
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "[data-product_id]", "data-product_id");

            saveDataProduct(null, internalPid, productUrl);
            this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);

            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }
   }

   @Override
   protected boolean hasNextPage() {
      return !this.currentDoc.select(".page-numbers a.next").isEmpty();
   }
}
