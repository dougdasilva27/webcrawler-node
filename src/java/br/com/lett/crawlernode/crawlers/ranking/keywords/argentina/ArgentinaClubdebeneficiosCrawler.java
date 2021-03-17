package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ArgentinaClubdebeneficiosCrawler extends CrawlerRankingKeywords {

   public ArgentinaClubdebeneficiosCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      // número de produtos por página do market
      this.pageSize = 24;

      this.log("Página " + this.currentPage);

      // monta a url com a keyword e a página
      String url = "https://clubdebeneficios.com/catalogsearch/result/index/?p=" + this.currentPage + "&q=" + this.keywordWithoutAccents.replace(" ", "%20");

      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select("ol.products.list.items.product-items.defer-images-grid>li");

      // se obter 1 ou mais links de produtos e essa página tiver resultado faça:
      if (!products.isEmpty()) {
         for (Element e : products) {
            String internalPid = e.attr("data-product-id");
            String urlProduct = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "div.product-item-info>a", "href");

            saveDataProduct(null, internalPid, urlProduct);

            this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + urlProduct);
            if (this.arrayProducts.size() == productsLimit)
               break;

         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {
      return this.currentDoc.select("li.item.pages-item-next") != null;
   }
}
