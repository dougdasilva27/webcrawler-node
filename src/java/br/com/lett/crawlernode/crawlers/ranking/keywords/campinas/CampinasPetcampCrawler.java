package br.com.lett.crawlernode.crawlers.ranking.keywords.campinas;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class CampinasPetcampCrawler extends CrawlerRankingKeywords {

   public CampinasPetcampCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 12;

      this.log("Página " + this.currentPage);

      String url = "https://www.petcamp.com.br/" + this.keywordWithoutAccents.replace(" ", "%20") + "#" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".sawi-shelf.n12colunas > ul > li:nth-child(odd)");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }
         for (Element e : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".sawi-shelf-box > .buy-button-normal", "id");
            String productUrl = CrawlerUtils.scrapUrl(e, ".sawi-shelf-inner > h3 > a", "href", "https:", "www.petcamp.com.br");
            String name = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".sawi-shelf-inner > h3 > a", "title");
            String imageUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".sawi-shelf-image > a > img", Arrays.asList("src"), "https", "www.petcamp.com.br");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".sawi-shelf-inner > p > .new-price", null, true, ',', session, null);
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
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

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }
   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapSimpleInteger(currentDoc, ".searchResultsTime:first-child > .resultado-busca-numero > .value", false);
      this.log("Total da busca: " + this.totalProducts);
   }
}
