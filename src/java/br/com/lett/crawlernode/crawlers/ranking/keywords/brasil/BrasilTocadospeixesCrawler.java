package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilTocadospeixesCrawler extends CrawlerRankingKeywords {

   public BrasilTocadospeixesCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.log("Página " + this.currentPage);

      String url = "https://www.tocadospeixes.com.br/buscar?q=" + this.keywordEncoded;


      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select("#listagemProdutos ul li ul li");

      if (!products.isEmpty()) {

         if (this.totalProducts == 0) {
            this.totalProducts = products.size();
         }
         for (Element e : products) {
            String internalId = crawlInternalId(e);
            String internalPid = crawlInternalPid(e);
            String productUrl = crawlProductUrl(e);

            saveDataProduct(internalId, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: "
               + internalPid + " - Url: " + productUrl);
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


   private String crawlInternalId(Element e) {
         return CrawlerUtils.scrapStringSimpleInfoByAttribute(e,"div[data-trustvox-product-code]","data-trustvox-product-code");
   }

   private String crawlInternalPid(Element e) {
      return CrawlerUtils.scrapStringSimpleInfo(e,".produto-sku",false);
   }

   private String crawlProductUrl(Element e) {
      return CrawlerUtils.scrapStringSimpleInfoByAttribute(e,"a","href");
   }

}
