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

public class NescafedolcegustoCrawler extends CrawlerRankingKeywords {
   public NescafedolcegustoCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      String url = "https://www.nescafe-dolcegusto.com.br/catalogsearch/result/?q=" + this.keywordWithoutAccents;
      this.log("Página " + this.currentPage);
      this.currentDoc = fetchDocument(url);
      this.log("Link onde são feitos os crawlers: " + url);

      Elements products = this.currentDoc.select(".products-listing__list.products > li");

      if (!products.isEmpty()) {

         for (Element product : products) {
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".price-box.price-final_price", "data-product-id");
            String productUrl = CrawlerUtils.scrapUrl(product, ".product-card__name--link", "href", "https", "www.nescafe-dolcegusto.com.br");
            String productName = CrawlerUtils.scrapStringSimpleInfo(product, ".product-card__name--link", false);
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".product-card__image", "data-hover-image");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(product, ".product-card__price-container > div > div.product-card__price--current > span > span > span", null, false, ',', session, null);
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
      return !this.currentDoc.select(".pages .items.pages-items .item.pages-item-next").isEmpty();
   }

}
