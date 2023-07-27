package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;

public class BrasilCasadoprodutorCrawler extends CrawlerRankingKeywords {

   public BrasilCasadoprodutorCrawler(Session session) {
      super(session);
   }

   @Override
   public void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 12;
      this.log("Página " + this.currentPage);

      String url = "https://www.casadoprodutor.com.br/catalogsearch/result/index/?p=" + this.currentPage + "&q=" + this.keywordEncoded;
      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".product-item");

      if (!products.isEmpty()) {
         for (Element e : products) {
            String productUrl = CrawlerUtils.scrapUrl(e, "a.product-item-photo", "href", "https", "www.casadoprodutor.com.br");
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".price-box.price-final_price", "data-product-id");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, "a.product-item-link", false);
            String imageUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".product-image-photo", Arrays.asList("src"), "https", "www.casadoprodutor.com.br");
            boolean isAvailable = e.select(".product.alert  .action.alert").isEmpty();
            Integer price = isAvailable ? CrawlerUtils.scrapPriceInCentsFromHtml(e, "[data-price-type=\"finalPrice\"] .price", null, false, ',', session, null) : null;

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
   protected boolean hasNextPage() {
      boolean hasNext;
      hasNext = !this.currentDoc.select(".next").isEmpty();

      return hasNext;
   }
}
