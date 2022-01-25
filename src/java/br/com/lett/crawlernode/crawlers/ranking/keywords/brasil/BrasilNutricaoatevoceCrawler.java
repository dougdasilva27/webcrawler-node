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
import java.util.Collections;

public class BrasilNutricaoatevoceCrawler extends CrawlerRankingKeywords {
   public BrasilNutricaoatevoceCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.log("Página " + this.currentPage);

      String url = "https://www.nutricaoatevoce.com.br/catalogsearch/result/?q=" + this.keywordEncoded;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".product-items > li");
      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            totalProducts = products.size();
         }

         for (Element product : products) {
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "form.add-product-to-cart", "data-product-sku");
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "div.product-item-details > form input[name=product]", "value");
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "div.product-item-info > div.image-container > a", "href");

            String name = CrawlerUtils.scrapStringSimpleInfo(product, ".product-item-name", false);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(product, ".product-image-photo", Collections.singletonList("src"), "https", "nutricaoatevoce.com.br");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(product, ".price", null, true, ',', session, 0);
            boolean isAvailable = price != 0;

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
            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");
   }
}
