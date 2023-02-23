package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;

public class ChileAritransCrawler extends CrawlerRankingKeywords {
   public ChileAritransCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 24;
      String url = "https://aritrans.cl/page/" + this.currentPage + "/?s=" + this.keywordEncoded + "&product_cat=0&post_type=product";
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".product-outer.product-item__outer");

      if (!products.isEmpty()) {

         for (Element product : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfo(product, ".product-sku", false).replace("SKU: ", "");
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".action-buttons > div", "data-fragment-ref");
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".product-loop-header.product-item__header > a", "href");
            String productName = CrawlerUtils.scrapStringSimpleInfo(product, ".woocommerce-loop-product__title", false);
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".product-thumbnail.product-item__thumbnail > img", "src").replace("-300x300", "");
            Integer price = getPrice(product);
            boolean isAvailable = product.selectFirst("div.product-loop-footer.product-item__footer > div.stock") != null;
            if (!isAvailable) {
               price = null;
            }

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(productName)
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
      this.log("Finalizando Crawler de produtos da página: " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private Integer getPrice(Element product) {
      Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(product, "div.price-add-to-cart > span > span > span > bdi", null, false, ',', session, 0);
      if (price == 0) {
         price = CrawlerUtils.scrapPriceInCentsFromHtml(product, ".woocommerce-Price-amount.amount", null, false, ',', session, 0);
      }
      return price;
   }

   @Override
   protected boolean hasNextPage() {
      return !this.currentDoc.select(".electro-advanced-pagination > a").isEmpty();
   }
}
