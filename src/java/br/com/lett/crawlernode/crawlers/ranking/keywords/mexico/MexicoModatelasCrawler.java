package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;

public class MexicoModatelasCrawler extends CrawlerRankingKeywords {
   public MexicoModatelasCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.log("Página " + this.currentPage);

      this.pageSize = 12;

      String url = "https://www.modatelas.com.mx/catalogsearch/result/?p=" + this.currentPage + "&" + "q=" + this.keywordEncoded.replace(" ", "+");

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url, cookies);

      Elements products = this.currentDoc.select(".products.wrapper.grid.columns4.products-grid > ol > li");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0)
            setTotalProducts();

         for (Element e : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product.details.product-item-details > div.price-box.price-final_price", "data-product-id");
            String productUrl = CrawlerUtils.completeUrl(CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product.details.product-item-details > strong > a", "href"), "https:", "www.modatelas.com.mx/");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".product.details.product-item-details > strong > a", true);
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product.photo.product-item-photo > a > img", "data-src");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".price-wrapper[data-price-type=finalPrice]", "data-price-amount", true, '.', session, null);
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(null)
               .setInternalId(internalId)
               .setName(name)
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

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {

      return this.currentDoc.selectFirst(".pages > ul > li.item.current > strong") != null;
   }
}
