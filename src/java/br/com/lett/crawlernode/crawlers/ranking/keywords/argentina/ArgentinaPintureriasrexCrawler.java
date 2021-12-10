package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class ArgentinaPintureriasrexCrawler extends CrawlerRankingKeywords {
   private String HOME_PAGE = "https://somosrex.com/";
   private Integer pageSize = 12;
   protected Integer PRODUCT_ID_SIZE = 7;

   public ArgentinaPintureriasrexCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      String url = HOME_PAGE + "catalogsearch/result/index/?p=" + this.currentPage + "&product_list_limit=" + pageSize + "&q=" + this.keywordEncoded;
      Integer currentPageUrl = (this.currentPage - 1) * this.pageSize;
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".item.product.product-item");
      if (!products.isEmpty()) {
         if (totalProducts == 0) {
            setTotalProducts();
         }
         for (Element e : products) {
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e,".product.photo.product-item-photo ", "href");
            String internalId = scrapInternalId(e);
            String name = CrawlerUtils.scrapStringSimpleInfo(e,".product-item-link", false);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".product-image-photo", Arrays.asList("src"), "", "");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".price-wrapper ", "data-price-amount", true, '.', session, 0);
            boolean isAvailable = price != 0;
            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setName(name)
               .setImageUrl(imgUrl)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);

         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }
   }

   private String scrapInternalId(Element e) {
      return CrawlerUtils.scrapStringSimpleInfoByAttribute(e,".price-box.price-final_price", "data-product-id");
   }

   @Override
   protected void setTotalProducts(){
      String totalProduct = CrawlerUtils.scrapStringSimpleInfo(this.currentDoc,".toolbar-amount", false);
      String [] arrProduct = totalProduct.split(" ");
      this.totalProducts = Integer.parseInt(arrProduct[(arrProduct.length) - 2]);
      this.log("Total: " + this.totalProducts);
   }
}
