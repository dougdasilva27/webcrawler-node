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



public class BrasilDrinksandclubsCrawlers extends CrawlerRankingKeywords {
   public BrasilDrinksandclubsCrawlers(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 16;

      String url = "https://www.drinksandclubs.com.br/buscapagina?ft="+this.keywordEncoded+"&PS=16&sl=bbceacdb-7240-46cf-94ba-d2cf18e3dca0&cc=4&sm=0&PageNumber="+this.currentPage;

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".shelf  > ul > li");
      int i = 0;
      for (Element product : products) {
         if (i % 2 == 0) {
            String productUrl = product.select(".product a").attr("href");
            String productName = CrawlerUtils.scrapStringSimpleInfo(this.currentDoc,".product__head",false);
            Integer productPrice = CrawlerUtils.scrapPriceInCentsFromHtml(this.currentDoc,".list-price span",null,true,',',session,null);
            String productImage = product.select("img").attr("src");
            String internalId = product.select("div[id_produto]").attr("id_produto");
            String outOfStockMessage = CrawlerUtils.scrapStringSimpleInfo(product, ".out-of-stock", false);
            Boolean isAvailable = outOfStockMessage == null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalId)
               .setName(productName)
               .setPriceInCents(productPrice)
               .setAvailability(isAvailable)
               .setImageUrl(productImage)
               .build();

            saveDataProduct(productRanking);
         }
         i++;
      }
   }

   @Override
   protected boolean hasNextPage() {
      return true;
   }
}
