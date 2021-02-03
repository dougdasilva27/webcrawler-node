package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;

//https://www.elabastecedor.com.ar/resultado.php?search=jugo&page=3
public class ArgentinaElabastecedorCrawler extends CrawlerRankingKeywords {
   private int totalPages = 0;


   public ArgentinaElabastecedorCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException {
      this.pageSize = 20;


      this.log("Página " + this.currentPage);


      String url = "https://www.elabastecedor.com.ar/resultado.php?search=" + this.keywordEncoded + "&page=" + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);
      this.totalPages = this.currentDoc.select(".pagination li").size();
      Elements products = this.currentDoc.select(".products-grid .itemCatalogo");

      //se obter 1 ou mais links de produtos e essa página tiver resultado faça:
      if (products.size() >= 1) {
         //se o total de busca não foi setado ainda, chama a função para setar
         if (this.totalProducts == 0) setTotalProducts();
         for (Element e : products) {

            // Url do produto
            String productUrl = crawlProductUrl(e);

            // InternalId
            String internalId = crawlInternalId(productUrl);

            // InternalPid
            String internalPid = internalId;

            saveDataProduct(internalId, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
            if (this.arrayProducts.size() == productsLimit) break;

         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

   }

   private String crawlProductUrl(Element e) {
      String url = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".item-img-info a", "href");
      return CrawlerUtils.completeUrl(url,"https:","www.elabastecedor.com.ar");
   }

   private String crawlInternalId(String url) {
      return CommonMethods.getLast(url.split("="));
   }

   @Override
   protected boolean hasNextPage() {
      return this.currentPage < this.totalPages;
   }
}
