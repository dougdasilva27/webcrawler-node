package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;

public class BrasilEmporioecoCrawler extends CrawlerRankingKeywords {

   public BrasilEmporioecoCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 12;
      this.log("Página : " + this.currentPage);

      String url = "https://lojaemporioeco.com.br/page/" + currentPage + "/?s=" + keywordEncoded + "&post_type=product";
      this.log("URL : " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".product_item--inner");

      if(!products.isEmpty()) {
         for(Element product : products) {
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".woocommerce-loop-product__link", "href");
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".wrap-addto a", "data-product_sku");
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".wrap-addto a", "data-product_id");
            String productName = CrawlerUtils.scrapStringSimpleInfo(product, ".product_item--title > a", true);
            Integer price = CrawlerUtils.scrapIntegerFromHtml(product, ".woocommerce-Price-amount", false, 0);
            String image = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "img", "src");
            boolean isAvailable = checkIfIsAvailable(product);

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalPid(internalPid)
               .setInternalId(internalId)
               .setName(productName)
               .setPriceInCents(price)
               .setImageUrl(image)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultados!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");
   }

   private boolean checkIfIsAvailable(Element product) {
      return product.select(".badge-out-of-stock").isEmpty();
   }

   @Override
   protected boolean hasNextPage() {
      return this.currentDoc.selectFirst(".next.page-numbers") != null;
   }
}
