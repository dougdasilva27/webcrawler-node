package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilPontonaturalCrawler extends CrawlerRankingKeywords  {
   private static final String HOME_PAGE = "www.pontonatural.com.br";

   public BrasilPontonaturalCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 28;
      this.log("Página " + this.currentPage);

      String url = "https://www.pontonatural.com.br/pesquisa?fc=module&module=iqitsearch&controller=searchiqit&s=" + this.keywordEncoded + "&page=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".products .js-product-miniature-wrapper");

      if (!products.isEmpty()) {
         for (Element e : products) {
            String productUrl = CrawlerUtils.scrapUrl(e, ".product-miniature .thumbnail-container .product-thumbnail", "href", "https", HOME_PAGE);
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-miniature", "data-id-product");
            saveDataProduct(internalId, null, productUrl);

            this.log(
               "Position: " + this.position +
                  " - InternalId: " + internalId +
                  " - InternalPid: " + null +
                  " - Url: " + productUrl);

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
   protected boolean hasNextPage() {
      return this.currentDoc.selectFirst(".page-list li:not(:first-child) .js-search-link")!= null;
   }
}
