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

public class BrasilDrogalCrawler extends CrawlerRankingKeywords {

   public BrasilDrogalCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 56;

      this.log("Página " + this.currentPage);
      String url = "https://www.drogal.com.br/" + this.keywordWithoutAccents.replaceAll(" ", "%20") + "/?p=" + this.currentPage;
      this.currentDoc = fetchDocument(url);

      this.log("Link onde são feitos os crawlers: " + url);

      Elements products = this.currentDoc.select(".list-products > div");

      if (!products.isEmpty()) {

         for (Element product : products) {
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".item-product", "data-sku");
            String productUrl = CrawlerUtils.scrapUrl(product, ".item-product > a", "href", "https", "www.drogal.com.br");
            String productName = CrawlerUtils.scrapStringSimpleInfo(product, ".title > a", false);
            String imageUrl = CrawlerUtils.completeUrl(CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".item-image > img", "data-src"), "https", "");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(product, ".sale-price > strong", null, false, ',', session, null);
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

   @Override
   protected boolean hasNextPage() {
      return !this.currentDoc.select(".page-item.next > a").isEmpty();
   }
}
