package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilAgrocidiCrawler extends CrawlerRankingKeywords {

   private static final String HOST = "www.agrocidi.com.br";

   public BrasilAgrocidiCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 40;
      this.log("Página " + this.currentPage);

      String url = "https://" + HOST + "/buscar?q=" + this.keywordEncoded + "&pagina=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select("#listagemProdutos .listagem-item");

      if (!products.isEmpty()) {
         for (Element e : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "[data-trustvox-product-code]", "data-trustvox-product-code");
            String internalPid = CrawlerUtils.scrapStringSimpleInfo(e, ".produto-sku", true);
            String productUrl = CrawlerUtils.scrapUrl(e, ".info-produto a", "href", "http:", HOST);

            saveDataProduct(null, internalPid, productUrl);

            this.log(
                  "Position: " + this.position +
                        " - InternalId: " + internalId +
                        " - InternalPid: " + internalPid +
                        " - Url: " + productUrl
            );

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
      return !this.currentDoc.select("li:not(.disabled) a[rel=next]").isEmpty();
   }
}
