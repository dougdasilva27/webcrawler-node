package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;

public class BrasilDivvinoCrawler extends CrawlerRankingKeywords {

   public BrasilDivvinoCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException {

      this.pageSize = 12;

      this.log("Página " + this.currentPage);

      int pageNumber = 0;

      if (currentPage != 1) {
         pageNumber = pageSize * this.currentPage;
      }
      //monta a url com a keyword e a página
      String url = "https://www.divvino.com.br/busca?No="+ pageNumber +"&Nrpp=12&Ntt=" + this.keywordEncoded;

      this.log("Link onde são feitos os crawlers: " + url);

      //chama função de pegar a url
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".container_products > div");

      //se obter 1 ou mais links de produtos e essa página tiver resultado faça:
      if (products.size() >= 1) {
         //se o total de busca não foi setado ainda, chama a função para setar
         if (this.totalProducts == 0) setTotalProducts();
         for (Element e : products) {


            // InternalId
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e,"[data-product]","data-product");

            // Url do produto
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e,"a","href");

            saveDataProduct(internalId, null, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
            if (this.arrayProducts.size() == productsLimit) break;

         }
      } else {
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

   }
}
