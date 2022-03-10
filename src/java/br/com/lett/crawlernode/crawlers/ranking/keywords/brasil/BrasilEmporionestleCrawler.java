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

public class BrasilEmporionestleCrawler extends CrawlerRankingKeywords {
   public BrasilEmporionestleCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 12;
      this.log("Página " + this.currentPage);
      String url = "https://www.emporionestle.com.br/catalogsearch/result/index/?p=" + this.currentPage + "&q=" + this.keywordEncoded;
      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select("div.product-item-info.product-item-details");
      pageSize = products.size();

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) setTotalProducts();
         for (Element e : products) {
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "div.product-item-info > a", "data-id");
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "div.product-item-info > a", "data-id");
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "div.product-item-info > a", "href");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".product.name .product-item-link", true);
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, "span[data-price-type = finalPrice]", null, false, ',', session, null);
            String imageUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".product-image-photo", Arrays.asList("src"), "https", "www.emporionestle.com.br");
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setImageUrl(imageUrl)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
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
      if (this.totalProducts == 0 && this.currentPage == 1) return true;

      return this.totalProducts > 0 && this.currentPage * this.pageSize < this.totalProducts;
   }

   @Override
   protected void setTotalProducts() {
      Element totalElement = this.currentDoc.selectFirst("#toolbar-amount > span:nth-child(3)");

      if (totalElement != null)
         this.totalProducts = Integer.parseInt(totalElement.text());

      this.log("Total da busca: " + this.totalProducts);
   }
}
