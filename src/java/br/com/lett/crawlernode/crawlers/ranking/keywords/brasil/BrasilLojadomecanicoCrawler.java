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

public class BrasilLojadomecanicoCrawler extends CrawlerRankingKeywords {
   public BrasilLojadomecanicoCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 48;

      this.log("Página " + this.currentPage);

      String key = this.keywordWithoutAccents.replaceAll(" ", "+");
      String url = "https://www.lojadomecanico.com.br/busca?q=" + key + "&page=" + this.currentPage;

      this.log("Url: " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".position-relative .container__showcase");
      pageSize = products.size();

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) setTotalProducts();
         for (Element e : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "[name=\"product\"]", "value");
            String internalPid = null;
            String productUrl = CrawlerUtils.scrapUrl(e, "a.showcase__link-img", "href", "https", "www.lojadomecanico.com.br");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".product-description a", true);
            String imageUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".img-responsive", Arrays.asList("src"), "https", "img.lojadomecanico.com.br");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, "p.price", null, false, ',', session, null);
            boolean isAvailable = e.selectFirst(".add-to-cart-btn") != null;
            if (!isAvailable) {
               price = null;
            }

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
      return (this.currentPage * this.pageSize) >= this.totalProducts;
   }

   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(currentDoc, ".neemu-total-products-container", true, 0);
      this.log("Total da busca: " + this.totalProducts);
   }
}
