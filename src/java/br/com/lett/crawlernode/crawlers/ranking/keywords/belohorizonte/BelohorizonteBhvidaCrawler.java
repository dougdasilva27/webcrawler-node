package br.com.lett.crawlernode.crawlers.ranking.keywords.belohorizonte;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

import java.util.Arrays;

public class BelohorizonteBhvidaCrawler extends CrawlerRankingKeywords {

   public BelohorizonteBhvidaCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {

      //For some reason this website loads only one page with all the products. I cannot find any search term with pagination.
      this.log("Página " + this.currentPage);

      String url = "https://www.bhvida.com/?LISTA=procurar&PROCURAR=" + this.keywordEncoded;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select("div.row.list div.grid-item");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0){
            setTotalProducts();
         }

         for (Element e : products) {
            String productUrl = "https://www.bhvida.com/" + CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "a.block2-name", "href");
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "div.grid-item", "id");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, "div.block2-txt  > a", false);
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, "span.block2-price strong", null, true, ',', session, null);
            if( price == null){
               price = CrawlerUtils.scrapPriceInCentsFromHtml(e, "span.block2-price", null, true, ',', session, null);
            }
            String imageUrl = CrawlerUtils.scrapSimplePrimaryImage(e, "div.block2-img > img", Arrays.asList("src"), "https", "www.bhvida.com");
            boolean isAvailable = CrawlerUtils.scrapStringSimpleInfo(e, "button[data-titulo]", true) != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(null)
               .setInternalPid(internalPid)
               .setImageUrl(imageUrl)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);
            if (this.arrayProducts.size() == productsLimit) {
               break;
            }

         }

      } else {

         this.result = false;
         this.log("Keyword sem resultado!");
      }

   }

   @Override
   protected void setTotalProducts() {
      Elements products = this.currentDoc.select("div.row.list div.grid-item");
      this.totalProducts = products.size();
      this.log("Total da busca: " + this.totalProducts);
   }
}
