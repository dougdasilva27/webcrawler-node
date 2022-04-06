package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.MathUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;

public class SaopauloNdaysCrawler extends CrawlerRankingKeywords {

   public SaopauloNdaysCrawler(Session session){
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {

      this.log("Página " + this.currentPage);
      String url = "https://www.ndays.com.br/index.php?route=product/search&search=" + this.keywordEncoded + "&page=" + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select("#content .product-layout");

      if(products.size() > 0){

         if(this.totalProducts == 0){
            this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, "#text-pagination-result", "Total", "(", false, false, 0);
            this.log("Total da busca: " + this.totalProducts);
         }

         for(Element product:products){

            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".button-group button", "data-id");
            Boolean isAvailable = false;
            Integer price = null;
            String[] nonFormattedUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".caption a", "href").split("\\?search");
            String urlProduct = nonFormattedUrl.length > 0 ? nonFormattedUrl[0] : null;
            String name = CrawlerUtils.scrapStringSimpleInfo(product, ".caption > h4 > a", false);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(product, "a .img-responsive", Arrays.asList("src"), "", "");
            Double priceDouble = CrawlerUtils.scrapDoublePriceFromHtml(product, ".price", null, false, ',', session);
            if(priceDouble == null){
               priceDouble = CrawlerUtils.scrapDoublePriceFromHtml(product, ".price .price-new", null, false, ',', session);
            }
            if(priceDouble != null){
               isAvailable = true;
               price = Math.toIntExact(Math.round(priceDouble * 100));
            }
            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(urlProduct)
               .setInternalId(internalId)
               .setName(name)
               .setImageUrl(imgUrl)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();
            saveDataProduct(productRanking);

            if (this.arrayProducts.size() == productsLimit) break;
         }

      } else{
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {
      return arrayProducts.size() < this.totalProducts;
   }
}
