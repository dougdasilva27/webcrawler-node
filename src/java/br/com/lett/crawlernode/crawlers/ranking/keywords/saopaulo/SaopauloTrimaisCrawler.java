package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Collections;

public class SaopauloTrimaisCrawler extends CrawlerRankingKeywords {

   public SaopauloTrimaisCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 24;
      this.log("Página " + this.currentPage);

      String url = "https://www.trimais.com.br/" + this.location.replace(" ", "%20") + "/?p=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".item-product");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0)
            setTotalProducts();
         for (Element e : products) {
            String productUrl = CrawlerUtils.scrapUrl(e, ".item-image", "href", "https", "www.trimais.com.br");
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".item-product", "data-sku");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, "h2.title a", true);
            String imageUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".item-image", Collections.singletonList("src"), "https", "io.convertiez.com.br");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".sale-price strong", null, true, ',', session, null);
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
               .build();

            saveDataProduct(productRanking);

            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".page-template .text-center", true, 0);
      this.log("Total: " + this.totalProducts);
   }
}
