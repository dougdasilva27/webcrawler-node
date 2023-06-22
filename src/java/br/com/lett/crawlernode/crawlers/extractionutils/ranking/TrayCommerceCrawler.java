package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

abstract public class TrayCommerceCrawler extends CrawlerRankingKeywords {

   protected final String homePage = setHomePage();
   protected final String storeId = setStoreId();

   protected abstract String setStoreId();

   public TrayCommerceCrawler(Session session) {
      super(session);
   }

   protected abstract String setHomePage();

   @Override
   public void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 20;
      this.log("Página " + this.currentPage);
      String url = homePage + "loja/busca.php?loja=" + storeId + "&palavra_busca=" + this.keywordEncoded + "&pg=" + this.currentPage;
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".product");

      if (!products.isEmpty()) {

         for (Element product : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "meta[itemprop=\"productID\"]", "content");
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "[itemprop=\"url\"]", "href");
            String productName = CrawlerUtils.scrapStringSimpleInfo(product, ".product-name > h3", true);
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".product-image > img", "src");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(product, ".product-price > span", null, false, ',', session, null);
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalId)
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
      return !this.currentDoc.select(".page-next > a").isEmpty();
   }

}
