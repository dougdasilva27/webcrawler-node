package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class MexicoSorianaCrawler extends CrawlerRankingKeywords {

   public MexicoSorianaCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      //número de produtos por página do market
      this.pageSize = 20;

      this.log("Página " + this.currentPage);

      //monta a url com a keyword e a página
      // primeira página começa em 0 e assim vai.

      String url = "https://www.soriana.com/soriana/es/search?q=" + this.keywordEncoded + "&page=" + (this.currentPage - 1);
      this.log("Link onde são feitos os crawlers: " + url);

      //chama função de pegar a url
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".product-item");

      //se obter 1 ou mais links de produtos e essa página tiver resultado faça:
      if (products.size() >= 1) {
         //se o total de busca não foi setado ainda, chama a função para setar
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {

            // InternalId
            String internalId = crawlInternalId(e);

            // Url do produto
            String productUrl = crawlProductUrl(e);

            saveDataProduct(internalId, null, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {
      return this.currentDoc.selectFirst(".pagination-next.disabled") == null;

   }

   @Override
   protected void setTotalProducts() {
      Element totalElement = this.currentDoc.select(".results h1").first();

      if (totalElement != null) {
         try {
            this.totalProducts = Integer.parseInt(totalElement.text().replaceAll(this.keywordWithoutAccents, "").replaceAll("[^0-9]", "").trim());
         } catch (Exception e) {
            this.logError(e.getMessage());
         }

         this.log("Total da busca: " + this.totalProducts);
      }
   }

   private String crawlInternalId(Element e) {
      String internalId = null;
      Element id = e.select("input[name=productCodePost]").first();

      if (id != null) {
         internalId = id.attr("value");
      }

      return internalId;
   }


   private String crawlProductUrl(Element e) {
      String urlProduct = null;
      Element urlElement = e.select(" > a").first();

      if (urlElement != null) {
         urlProduct = urlElement.attr("href");

         if (!urlProduct.startsWith("https://www.soriana.com")) {
            urlProduct = "https://www.soriana.com" + urlProduct;
         }
      }

      return urlProduct;
   }
}
