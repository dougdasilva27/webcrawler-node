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

public class BrasilFarmaPonteCrawler extends CrawlerRankingKeywords {
   public BrasilFarmaPonteCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      String newKeywordEncoded = this.keywordEncoded.replace("+", "%20");
      String url = "https://www.farmaponte.com.br/" + newKeywordEncoded + "/?p=" + this.currentPage;
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".list-products.page-content li");
      if (!products.isEmpty()) {
         for (Element product : products) {
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".item-product", "data-sku");
            String productUrl = CrawlerUtils.scrapUrl(product, ".item-product .link.image", "href", "https", "www.farmaponte.com.br");
            String productName = CrawlerUtils.scrapStringSimpleInfo(product, ".item-product .desc .title", false);
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".item-product .link.image img", "data-src");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(product,  ".item-product .desc .box-prices .prices .sale_price", null,  false, ',', session, null);
            Integer priceFrom = CrawlerUtils.scrapPriceInCentsFromHtml(product, ".item-product .desc .box-prices .prices .price_off", null, false, ',', session, price);
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalPid(internalPid)
               .setName(productName)
               .setPriceInCents(price)
               .setPriceInCents(priceFrom)
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
      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {
      return !this.currentDoc.select(".pagination ul .next .active.link").isEmpty();
   }
}
