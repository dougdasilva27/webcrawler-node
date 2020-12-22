package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilCasadoprodutorCrawler extends CrawlerRankingKeywords {

   public BrasilCasadoprodutorCrawler(Session session) {
      super(session);
   }

   @Override
   public void extractProductsFromCurrentPage() {
      this.pageSize = 24;
      this.log("Página " + this.currentPage);

      String keyword = this.keywordWithoutAccents.replace(" ", "");

      String url = "https://www.casadoprodutor.com.br/catalogsearch/result/index/?cat=0&p=" + this.currentPage + "&q=" + keyword;
      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);


      Elements products = this.currentDoc.select(".category-products .products-grid li");

      if (!products.isEmpty()) {
         for (Element element : products) {
            String internalId = scrapInternalId(element);
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(element, "a", "href");
            saveDataProduct(internalId, null, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - Url: " + productUrl);
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }
      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {
      boolean hasNext = false;
      hasNext = !this.currentDoc.select(".next").isEmpty();


      return hasNext;
   }


   private String scrapInternalId(Element element) {
      String internalId = null;
      String attribute = CrawlerUtils.scrapStringSimpleInfoByAttribute(element, ".regular-price", "id");
      if (attribute != null && attribute.contains("product-price")) {
         internalId = attribute.replace("product-price-", "");
      }
      return internalId;
   }

}
