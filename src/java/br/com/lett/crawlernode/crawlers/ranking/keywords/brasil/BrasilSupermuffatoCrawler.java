package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilSupermuffatoCrawler extends CrawlerRankingKeywords {

   private static final String BASE_URL = "www.supermuffato.com.br/";

   public BrasilSupermuffatoCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 24;
      this.log("Página " + this.currentPage);

      String url = "https://www.supermuffato.com.br/buscapagina?" +
         "ft=" + this.keywordEncoded +
         "&PS=24" +
         "&sl=4fb62b72-d10b-407c-ab22-24e271c4df75" +
         "&cc=24" +
         "&sm=0" +
         "&PageNumber=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);

      //chama função de pegar a url
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select("li[layout]");
      Elements productsIdList = this.currentDoc.select("li[id].helperComplement");

      if (products.size() >= 1) {

         if (this.totalProducts == 0)
            setTotalProducts();

         for (int index = 0; index<products.size(); index++) {
            Element product = products.get(index);

            String internalId = null;
            String internalPid = crawlInternalPid(productsIdList.get(index));
            String urlProduct = CrawlerUtils.scrapUrl(product, ".prd-list-item-desc > a", "href", "https", BASE_URL);

            saveDataProduct(internalId, internalPid, urlProduct);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + urlProduct);
            if (this.arrayProducts.size() == productsLimit) break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
         setTotalProducts();
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
      if (!(hasNextPage())) setTotalProducts();
   }

   @Override
   protected void setTotalProducts() {
      Document html = fetchDocument("https://www.supermuffato.com.br/" + keywordEncoded);
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(html, ".resultado-busca-numero span.value", true, 0);
      this.log("Total da busca: " + this.totalProducts);
   }

   private String crawlInternalPid(Element productId) {
      String id = CrawlerUtils.scrapStringSimpleInfoByAttribute(productId, null, "id");
      String[] split = id.split("_");
      return split[1];
   }
}
