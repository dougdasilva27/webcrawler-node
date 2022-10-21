package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilMerceariadoanimalCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "https://www.merceariadoanimal.com.br/";

   public BrasilMerceariadoanimalCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 18;
      this.log("Página " + this.currentPage);

      String url = HOME_PAGE + "pesquisa.ecm?search_query=" + this.keywordWithoutAccents + "&x=0&y=0&page=" + this.currentPage + "&section=product#results";

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".ProductList.Clear > li");

      if (!products.isEmpty()) {

         for (Element product : products) {
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".ProductImage.QuickView", "data-product");
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".ProductImage.QuickView > a", "href");
            String productName = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "a.TrackLink > img", "alt");
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "div.ProductImage.QuickView > a > img", "src");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(product, "div.ProductPriceRating > em:nth-child(1)", null, false, ',', session, 0);
            String buttonAvailable = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".ProdutoNaoDisponivel", "style");
            boolean isAvailable = buttonAvailable != null && buttonAvailable.contains("display:none;");
            if(!isAvailable){
               price = null;
            }

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
      this.log("Finalizando Crawler de produtos da página: " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {
      return this.currentDoc.selectFirst(".CategoryPagination .FloatRight > a") != null;
   }
}
