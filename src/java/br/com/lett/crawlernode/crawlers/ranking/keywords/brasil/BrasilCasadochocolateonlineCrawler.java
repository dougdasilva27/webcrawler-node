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

public class BrasilCasadochocolateonlineCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "https://www.casadochocolateonline.com.br/produtos?q=";

   public BrasilCasadochocolateonlineCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.currentDoc = fetchDocument(HOME_PAGE);

      Elements products = this.currentDoc.select(".container > div.row.no-gutters > div.col.order-1 > section.flex-grow-1.mw-100 > div.container-fluid.mb-4 > div.col-6.col-sm-6.col-md-4.col-lg-4.mb-4");

      if(!products.isEmpty()){
         if(this.totalProducts == 0) {
            setTotalProducts();

            for(Element product : products){
               String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "review-stars.mt-0", "data-id");
               String productUrl = CrawlerUtils.scrapUrl(product, ".product-card.p-3.p-md-4.d-flex.flex-column.align-items-strech.h-100.position-relative.final a", "href", "https", "www.casadochocolateonline.com.br");
               String productName = CrawlerUtils.scrapStringSimpleInfo(product, ".product-title.text-center.mb-4 .h2",false);
               String imageUrl = "https:"+ CrawlerUtils.scrapStringSimpleInfoByAttribute(product, " a > figure > img", "loading");
               Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(product, ".product-card-price.m-final .price", null, false, ',', session, null);
               boolean isAvailable = price != null;

               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(internalId)
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
            this.log("Not a found!");
         }
         this.log("Finishing page products crawler: " + this.currentPage + " - yet " + this.arrayProducts.size() + " crawled products");
      }
   }

   @Override
   protected boolean hasNextPage() {
      return !this.currentDoc.select(".paginate .paginate__item .link").isEmpty();
   }
}
