package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.MathUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilNutricaototalCrawler extends CrawlerRankingKeywords {
   public BrasilNutricaototalCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 12;

      this.log("Página " + this.currentPage);
      String key = this.keywordWithoutAccents.replaceAll(" ", "+");
      String url = "https://www.nutricaototal.com.br/catalogsearch/result/index/?p="
         + this.currentPage + "&q=" + key;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".product-item");

      if (products.size() >= 1) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {

            String internalId = e.selectFirst(".price-box").attr("data-product-id");
            String productUrl = e.selectFirst(".product-item-photo").attr("href");

            saveDataProduct(internalId, null, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - Url: " + productUrl);

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
   protected void setTotalProducts() {
      try {
         Element elemTotal = this.currentDoc.select(".toolbar-amount .toolbar-number").last();
         if (elemTotal != null) {
            this.totalProducts = MathUtils.parseInt(elemTotal.text());
         }
      } catch (Exception e) {
         this.logError(CommonMethods.getStackTraceString(e));
      }

      this.log("Total da busca: " + this.totalProducts);
   }

   private String crawlInternalId(Element e, String selector) {
      Element aux = e.selectFirst(selector);
      String internalId = null;

      if (aux != null) {
         String attr = aux.id();

         if (!attr.isEmpty()) {
            internalId = MathUtils.parseInt(attr).toString();
         }
      }

      return internalId;
   }

   private String crawlProductUrl(Element e, String selector) {
      Element aux = e.selectFirst(selector);
      String url = null;

      if (aux != null) {
         url = CrawlerUtils.sanitizeUrl(aux, "href", "https", "www.nutricaototal.com.br");
      }

      return url;
   }
}
