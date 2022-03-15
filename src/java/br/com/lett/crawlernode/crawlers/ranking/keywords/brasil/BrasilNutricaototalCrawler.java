package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.MathUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Collections;

public class BrasilNutricaototalCrawler extends CrawlerRankingKeywords {
   public BrasilNutricaototalCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 12;

      this.log("Página " + this.currentPage);
      String key = this.keywordWithoutAccents.replaceAll(" ", "+");
      String url = "https://www.nutricaototal.com.br/catalogsearch/result/index/?p="
         + this.currentPage + "&q=" + key;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".product.product-item");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {

            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".price-box", "data-product-id");
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-item-photo", "href");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".product-item-name", false);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".product-image-photo", Collections.singletonList("src"), "https", "nutricaototal.com.br");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, "span[data-price-type=finalPrice]", null, false, ',', session, 0);
            boolean isAvailable = e.selectFirst("div.stock.unavailable") == null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(null)
               .setImageUrl(imgUrl)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);
         }

      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected void setTotalProducts() {
      try {
         Element elemTotal = this.currentDoc.select(".toolbar-amount .toolbar-number").last();
         if (elemTotal != null) {
            this.totalProducts = MathUtils.parseInt(elemTotal.text());
         }
      } catch (Exception e) {
         this.logError(CommonMethods.getStackTraceString(e));
      }

      this.log("Total da busca: " + this.totalProducts);
   }
}
