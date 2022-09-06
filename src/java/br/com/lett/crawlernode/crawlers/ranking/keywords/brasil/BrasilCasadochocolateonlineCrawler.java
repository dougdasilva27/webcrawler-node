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

   public BrasilCasadochocolateonlineCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      String url = "https://www.casadochocolateonline.com.br/produtos?q="+ this.keywordEncoded + "&page=" + this.currentPage;
      this.currentDoc = fetchDocument(url);

     Elements products = this.currentDoc.select(".row.no-gutters .container-fluid.mb-4 .row.mt-4 .col-6");

      if(!products.isEmpty()){
         if(this.totalProducts == 0) {
            setTotalProducts();

            for(Element product : products){
               String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".review-stars.mt-0", "data-id");
               String productUrl = CrawlerUtils.scrapUrl(product, ".product-card.p-3.p-md-4.d-flex.flex-column.align-items-strech.h-100.position-relative.final a", "href", "https", "www.casadochocolateonline.com.br");
               String productName = CrawlerUtils.scrapStringSimpleInfo(product, ".product-card .product-title .text",false);
               String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".img-fluid", "src");
               Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(product, ".product-card-price.m-final .price", null, false, ',', session, null);
               Integer priceFrom = CrawlerUtils.scrapPriceInCentsFromHtml(product,".product-card-price .discount",null,false,',',session,price);
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
