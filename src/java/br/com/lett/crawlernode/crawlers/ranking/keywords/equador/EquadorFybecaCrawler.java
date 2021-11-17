package br.com.lett.crawlernode.crawlers.ranking.keywords.equador;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class EquadorFybecaCrawler extends CrawlerRankingKeywords {

   public EquadorFybecaCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {

      String url = "https://www.fybeca.com/busqueda?q=" + this.keywordEncoded + "&search-button=&lang=es_EC&start=" + this.arrayProducts.size() + "&sz=18&maxsize=18";
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".products-grid .col-12.col-lg-4");

      if (products != null) {
         for (Element product : products) {
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".product.product-wrapper", "data-pid");
            String productUrl = "https://www.fybeca.com" + CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".pdp-link .link", "href");
            String name = CrawlerUtils.scrapStringSimpleInfo(product, ".pdp-link .link", true);
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".tile-image.m-auto", "src");

            int price = CrawlerUtils.scrapIntegerFromHtml(product, ".value.pr-2", true, 0);
            price = price == 0 ? price : CrawlerUtils.scrapIntegerFromHtml(product, ".value.pr-2", true, 0);
            boolean isAvailable = price != 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalPid(internalPid)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
               .build();

            saveDataProduct(productRanking);

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
      return true;
   }
}
