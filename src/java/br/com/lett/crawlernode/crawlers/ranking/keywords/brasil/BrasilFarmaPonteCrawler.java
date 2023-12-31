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
import java.util.List;

public class BrasilFarmaPonteCrawler extends CrawlerRankingKeywords {
   public BrasilFarmaPonteCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      String newKeywordEncoded = this.keywordEncoded.replace("+", "%20");
      String url = "https://www.farmaponte.com.br/" + newKeywordEncoded + "/?p=" + this.currentPage;
      this.currentDoc = fetchDocument(url);
      
      Elements products = this.currentDoc.select(".container .list-products.page-content .li");
      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }
         for (Element product : products) {
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".item-product", "data-sku");
            String productUrl = CrawlerUtils.scrapUrl(product, ".item-product a.item-image", "href", "https", "www.farmaponte.com.br");
            String productName = CrawlerUtils.scrapStringSimpleInfo(product, ".item-product .desc .title", false);
            String imageUrl = scrapLargeImage(product);
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(product,  ".item-product .desc .box-prices .prices .pix-price", null,  false, ',', session, null);
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
      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".container .page-template .text-center.pt-3", false, 0);
   }

   private String scrapLargeImage(Element product) {
      String url = CrawlerUtils.scrapSimplePrimaryImage(product, ".item-product a.item-image img", List.of("data-src"), "https:", "");
      return url.replaceAll("/(mini|small|medium)/", "/large/");
   }
}
