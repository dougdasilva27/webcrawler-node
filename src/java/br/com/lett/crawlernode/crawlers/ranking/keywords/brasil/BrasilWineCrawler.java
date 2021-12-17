package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

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

public class BrasilWineCrawler extends CrawlerRankingKeywords {
   private String HOME_PAGE = "https://www.wine.com.br/";
   private Integer pageSize = 9;
   protected Integer PRODUCT_ID_SIZE = 7;

   public BrasilWineCrawler (Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      String url = HOME_PAGE + "search.ep?keyWords=" + this.keywordEncoded + "&exibirEsgotados=false&pn=" + this.currentPage + "&listagem=horizontal&sorter=relevance-desc&filters=";
      Integer currentPageUrl = (this.currentPage - 1) * this.pageSize;
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".ProductList-content ul li");
      if (!products.isEmpty()) {
         if (totalProducts == 0) {
            setTotalProducts();
         }
         for (Element e : products) {
            String productUrl = HOME_PAGE + CrawlerUtils.scrapStringSimpleInfoByAttribute(e,".js-productClick", "href");
            String internalId = scrapInternalId(e);
            String name = CrawlerUtils.scrapStringSimpleInfo(e,".js-productClick h2", false);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".ProductDisplay-picture img", Arrays.asList("src"), "", "");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".CartItem-image.lazy-img ", "data-fullprice", true, '.', session, 0);
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
      return CrawlerUtils.scrapStringSimpleInfoByAttribute(e,".pCartItem-image.lazy-img", "data-sku");
   }

   @Override
   protected void setTotalProducts(){
      String totalProduct = CrawlerUtils.scrapStringSimpleInfo(this.currentDoc,".HeaderProductList-text-number", false);
      this.totalProducts = Integer.parseInt( totalProduct);
      this.log("Total: " + this.totalProducts);
   }
}



