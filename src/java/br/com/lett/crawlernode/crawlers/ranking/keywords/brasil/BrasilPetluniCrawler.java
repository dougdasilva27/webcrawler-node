package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.Arrays;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilPetluniCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "www.petluni.com.br";

   public BrasilPetluniCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 12;
      this.log("Página " + this.currentPage);



      String url = "https://www.petluni.com.br/"+this.keywordWithoutAccents.replace(" ", "%20")+"#" + this.currentPage;

      this.currentDoc = fetchDocument(url);

      this.log("Link onde são feitos os crawlers: " + url);

      Elements products = this.currentDoc.select("[class*=productRow] ul > li:not(.helperComplement)");

      if (!products.isEmpty()) {
         for (Element e : products) {

            String productPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".productItem", "data-product-id");
            String productUrl = CrawlerUtils.scrapUrl(e, "a.productItem__link", Arrays.asList("href"), "https", HOME_PAGE);

            saveDataProduct(null, productPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + productPid + " - Url: " + productUrl);
            if (this.arrayProducts.size() == productsLimit)
               break;

         }
      } else if (this.arrayProducts.isEmpty()) {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }


   @Override
   protected boolean hasNextPage() {
      return this.currentDoc.select("[class*=productRow] ul > li:not(.helperComplement)").size() >= this.pageSize;
   }
}
