package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

import java.util.Collections;

public class BrasilDrogariasoaresCrawler extends CrawlerRankingKeywords {

   public BrasilDrogariasoaresCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 32;

      this.log("Página " + this.currentPage);

      // we put 100 on "pagina" because this site returns all the products on the last page
      String url = "https://drogariasoares.com.br/busca/" + this.keywordWithoutAccents.replace(" ", "-") + "?pagina=100";
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".produtos-lista .item-prod");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(currentDoc, ".produtos-lista .row .columns.large-12 p", "de", "res", true, true, 0);
            this.log("Total: " + this.totalProducts);
         }

         for (Element e : products) {
            String internalId = crawlInternalId(e);
            String productUrl = CrawlerUtils.scrapUrl(e, ".link-prod", "href", "https", "drogariasoares.com.br");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".nome h1", true);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".imagem img", Collections.singletonList("src"), "https", "drogariasoares.com.br");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".preco .preco-por", null, false, ',', session, null);
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(null)
               .setImageUrl(imgUrl)
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

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {
      return false;
   }

   private String crawlInternalId(Element e) {
      String internalId = null;

      String spyurl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "button[data-reveal-ajax]", "data-reveal-ajax");
      if (spyurl != null) {
         internalId = CommonMethods.getLast(spyurl.split("/"));
      }

      return internalId;
   }
}
