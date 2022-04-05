package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

import java.util.Collections;

public class ArgentinaOpenfarmaCrawler extends CrawlerRankingKeywords {


   public ArgentinaOpenfarmaCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {

      this.pageSize = 12;

      this.log("Página " + this.currentPage);

      String url = "https://www.openfarma.com.ar/products?utf8=%E2%9C%93&keywords=" + this.keywordEncoded + "&page=" + this.currentPage + "&utf8=%E2%9C%93";


      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".card-product");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) setTotalProducts();
         for (Element e : products) {
            String internalPid = null;
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "#variant_id", "value");
            String productUrl = CrawlerUtils.scrapUrl(e, "a", "href", "https", "www.openfarma.com.ar");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, "h2.card-title", true);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".card-thumb img", Collections.singletonList("src"), "https", "dqm4sv5xk0oaj.cloudfront.net");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".card-prices .promo", null, false, ',', session, null);
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setImageUrl(imgUrl)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);

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
      return this.currentDoc.selectFirst(".pagination .page:nth-child(2)") != null;
   }

}
