package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;

public class MexicoOfficedepotCrawler extends CrawlerRankingKeywords {
   private final String HOME_PAGE = "https://www.officedepot.com.mx/officedepot/en/";

   public MexicoOfficedepotCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      String url = HOME_PAGE + "search/?q=" + keywordWithoutAccents;
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".product-item");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }
         for (Element product : products) {
            String productName = CrawlerUtils.scrapStringSimpleInfo(product, ".name.description-style > h2", true);
            String productUrl = CrawlerUtils.scrapUrl(product, ".product-description", "href", "https", "www.officedepot.com.mx");
            String imageUrl = CrawlerUtils.scrapUrl(product, ".thumb.center-content-items > img", "data-src", "https", "www.officedepot.com.mx");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(product, ".btn-primary-rs", null, false, '.', session, null);
            String internalPid = CrawlerUtils.scrapStringSimpleInfo(product, ".product-sku > span.name-add.font-medium", false);
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
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
}
