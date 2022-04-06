package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;

/**
 * Date: 22/01/21
 *
 * @author Fellype Layunne
 */
public class ArgentinaFarmaonlineCrawler extends CrawlerRankingKeywords {

   public ArgentinaFarmaonlineCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   public String getHomePage() {
      return br.com.lett.crawlernode.crawlers.corecontent.argentina.ArgentinaFarmaonlineCrawler.HOME_PAGE;
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {

      if (this.currentPage == 1) {
         setTotalProducts();
      }

      this.pageSize = 48;
      String url = getHomePage() + "buscapagina" +
         "?ft=" + keywordEncoded +
         "&PS=" + this.pageSize +
         "&cc=" + this.pageSize +
         "&sl=ef3fcb99-de72-4251-aa57-71fe5b6e149f" +
         "&sm=0" +
         "&PageNumber=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".prateleira ul li span[data-id]");

      if (!products.isEmpty()) {

         for (Element e : products) {
            String internalPid = e.attr("data-id");
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "a", "href");

            String name = CrawlerUtils.scrapStringSimpleInfo(e, "span > h3 > a", true);
            String imageUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".span > a > img", Arrays.asList("src"), "https", "www.farmaonline.com");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, "span > .price > a > .best-price", null, true, ',', session, null);
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalPid(internalPid)
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
      String url = getHomePage() + keywordWithoutAccents.replace(" ", "%20");
      Document doc = fetchDocument(url);

      totalProducts = CrawlerUtils.scrapIntegerFromHtml(doc, ".resultado-busca-numero .value", false, 0);
   }
}
