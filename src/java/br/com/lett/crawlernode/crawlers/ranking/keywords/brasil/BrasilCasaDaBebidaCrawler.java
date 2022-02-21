package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilCasaDaBebidaCrawler extends CrawlerRankingKeywords {
   protected Integer PRODUCTS_PER_PAGE = 20;
   private static final String HOME_PAGE = "https://www.casadabebida.com.br";

   public BrasilCasaDaBebidaCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.log("Página " + this.currentPage);

      String url = HOME_PAGE + "/pesquisa/pagina-" + this.currentPage + "/?search=" + this.keywordEncoded;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".col-md-10 .row .produtos-categoria-thumb");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            this.totalProducts = products.size();
         }

         for (Element e : products) {
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e,".float-left", "href");
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e,"[name=\"productIds[]\"]", "value");
            String name = CrawlerUtils.scrapStringSimpleInfo(e,".product-thumb-info a", false);
            String imgUrlPath = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "picture img", "data-src");
            String imgUrl = HOME_PAGE.concat(imgUrlPath);
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".price span:nth-child(2)", null, false, ',', session, 0);

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

            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");

   }

   @Override
   protected boolean hasNextPage(){
      return true;
   }
}
