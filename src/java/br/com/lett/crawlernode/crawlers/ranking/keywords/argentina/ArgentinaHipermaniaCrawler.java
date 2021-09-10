package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.InternalIdNotFound;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ArgentinaHipermaniaCrawler extends CrawlerRankingKeywords {

   public ArgentinaHipermaniaCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 16;

      this.log("Página " + this.currentPage);

      String url = "https://www.hipermania.com.ar/page/" + this.currentPage + "/?s=" + this.keywordEncoded + "&post_type=product";
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".products.columns-4 li");

      for (Element e : products) {
         String productUrl = CrawlerUtils.scrapUrl(e, ".woocommerce-LoopProduct-link", "href", "https", "www.hipermania.com.ar");
         String internalId = e.classNames().stream().filter(s -> s.matches("post-[0-9^]*")).findFirst()
            .map(s -> s.replaceAll("[^0-9]", ""))
            .orElseThrow(InternalIdNotFound::new);

         saveDataProduct(internalId, null, productUrl);
         this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
      }
   }

   @Override
   protected boolean hasNextPage() {
      return !this.currentDoc.select(".next.page-numbers").isEmpty();
   }
}
