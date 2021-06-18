package br.com.lett.crawlernode.crawlers.ranking.keywords.belohorizonte;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BelohorizonteSantahelenaCrawler extends CrawlerRankingKeywords {

   public BelohorizonteSantahelenaCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 16;

      this.log("Página " + this.currentPage);

      String url = "https://santahelenacenter.com.br/page/" + this.currentPage + "/?s=" + this.keywordEncoded + "&post_type=product&dgwt_wcas=1";
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".products.columns-4 li");
      if (products.size() >= 1) {

         for (Element e : products) {
            String productUrl = CrawlerUtils.scrapUrl(e, ".container-inner a", "href", "https", "santahelenacenter.com.br");
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "form.cart", "action").replaceAll("[^0-9]", "").trim();

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
   }

   @Override
   protected boolean hasNextPage() {
      return !this.currentDoc.select(".next.page-numbers").isEmpty();
   }
}
