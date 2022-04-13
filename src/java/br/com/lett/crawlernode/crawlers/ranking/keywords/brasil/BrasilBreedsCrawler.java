package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.Arrays;
import java.util.Collections;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilBreedsCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "www.breeds.com.br/";

   public BrasilBreedsCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 21;
      this.log("Página " + this.currentPage);

      String url = "https://" + HOME_PAGE + "catalogsearch/result/index/?p=" + this.currentPage + "&q=" + this.keywordEncoded;
      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".products-grid .item");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            Integer internalPidInt = CrawlerUtils.scrapIntegerFromHtmlAttr(e, ".regular-price", "id", null);
            String internalPid = internalPidInt != null ? internalPidInt.toString() : null;
            String productUrl = CrawlerUtils.scrapUrl(e, ".item a", Arrays.asList("href"), "https:", HOME_PAGE);
            String name = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-name a", "title");
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".product-image img", Collections.singletonList("src"), "https", "cdn.breeds.com.br");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".regular-price .price", null, true, ',', session, 0);
            boolean isAvailable = e.selectFirst(".out-of-stock") == null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(null)
               .setInternalPid(internalPid)
               .setImageUrl(imgUrl)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);

            if (this.arrayProducts.size() == productsLimit)
               break;
         }

      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
            + this.arrayProducts.size() + " produtos crawleados");

   }

   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".amount.amount--has-pages", "de", "", true, false, 0);

      this.log("Total de produtos: " + this.totalProducts);
   }
}
