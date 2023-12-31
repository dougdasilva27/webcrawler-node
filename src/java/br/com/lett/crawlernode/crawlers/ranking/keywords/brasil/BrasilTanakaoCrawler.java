package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;

public class BrasilTanakaoCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "tanakao.com.br";

   public BrasilTanakaoCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 24;
      this.log("Página " + this.currentPage);

      String url = "https://www.tanakao.com.br/catalogsearch/result/?p=" + this.currentPage + "&q=" + this.keywordEncoded;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".products-wrapper .item");

      if (!products.isEmpty()) {

         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String pidAllString = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".price-box .regular-price", "id");
            String internalPid = pidAllString != null && pidAllString.contains("product-price-")? pidAllString.split("price-")[1]: null;
            String productUrl = CrawlerUtils.scrapUrl(e, "a", Arrays.asList("href"), "https", HOME_PAGE);

            saveDataProduct(null, internalPid, productUrl);

            this.log(
               "Position: " + this.position +
                  " - InternalId: " + null +
                  " - InternalPid: " + internalPid +
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
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".pager .amount", "do", "total", true, true, 0);

      this.log("Total de produtos: " + this.totalProducts);
   }
}
