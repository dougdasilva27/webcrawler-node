package br.com.lett.crawlernode.crawlers.ranking.keywords.espana;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class EspanaTiendaAnimalCrawler extends CrawlerRankingKeywords {

   public EspanaTiendaAnimalCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 12;
      this.log("Página " + this.currentPage);

      String url = "https://www.tiendanimal.es/on/demandware.store/Sites-TiendanimalES-Site/default/Search-UpdateGrid?q=" + this.keywordEncoded + "&start=" + (this.currentPage - 1) * this.pageSize + "&sz=" + this.pageSize + "&selectedUrl=%2Fbuscador%3Fq%3D" + this.keywordEncoded + "%26lang%3Ddefault";

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".product.js-product-tile");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) setTotalProducts();
         for (Element e : products) {
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, null, "data-pid");
            String productUrl = CrawlerUtils.scrapUrl(e, ".js-product-tile-anchor ", "href", "https", "www.tiendanimal.es");
            String internalId = null;
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".gtm-product-name", true);
            String image = null;
            boolean available = false;
            Integer priceInCents = null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setImageUrl(image)
               .setName(name)
               .setPriceInCents(priceInCents)
               .setAvailability(available)
               .build();

            saveDataProduct(productRanking);
         }
      }
      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

   }

   @Override
   protected boolean hasNextPage() {
      return !this.currentDoc.select(".show-more.mt-1").isEmpty();
   }
}
