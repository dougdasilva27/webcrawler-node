package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ArgentinaMillanCrawler extends CrawlerRankingKeywords {

   public ArgentinaMillanCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
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
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-title a", "href");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".product-title a", true);
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".card-img-top.product__card-img a img", "data-src");
            Integer price = CrawlerUtils.scrapIntegerFromHtml(e, ".product-price-and-shipping.text-center .price", true, null);
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(null)
               .setInternalPid(internalPid)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
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
      return this.currentDoc.selectFirst("a[rel=next]") != null;
   }

   @Override
   protected void setTotalProducts() {
      Element totalElement = this.currentDoc.select("div.visible--mobile.text-sm-center.mt-1.col-12 ").first();

      if (totalElement != null) {
         String[] splittedStr = totalElement.html().split(" ");

         if (splittedStr.length > 0) {
            this.totalProducts = Integer.parseInt(splittedStr[3]);
         }

         this.log("Total da busca: " + this.totalProducts);
      }
   }
}
