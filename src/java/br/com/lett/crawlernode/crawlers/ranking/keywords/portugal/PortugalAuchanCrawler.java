package br.com.lett.crawlernode.crawlers.ranking.keywords.portugal;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class PortugalAuchanCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "https://www.auchan.pt/Frontoffice/";


   public PortugalAuchanCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {

      this.pageSize = 24;
      this.log("Página " + this.currentPage);

      String urlFirstPage = "https://www.auchan.pt/pt/pesquisa?q=" + this.keywordEncoded + "&search-button=&lang=pt_PT";
      String url = "https://www.auchan.pt/on/demandware.store/Sites-AuchanPT-Site/pt_PT/Search-UpdateGrid?q=" + this.keywordEncoded + "&prefn1=soldInStores&prefv1=000&start=" + (this.currentPage - 1) * 24 + "&sz=24&selectedUrl=https%3A%2F%2Fwww.auchan.pt%2Fon%2Fdemandware.store%2FSites-AuchanPT-Site%2Fpt_PT%2FSearch-UpdateGrid%3Fq%3D" + this.keywordEncoded + "%26prefn1%3DsoldInStores%26prefv1%3D000%26start%3D" + (this.currentPage - 1) * 24 + "%26sz%3D24";
      this.log("Link onde são feitos os crawlers: " + url);

      if (this.currentPage != 1) {
         this.currentDoc = fetchDocument(url);
      } else {
         this.currentDoc = fetchDocument(urlFirstPage);
      }

      Elements products = this.currentDoc.select(".auc-product");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product", "data-pid");
            String productUrl = CrawlerUtils.scrapUrl(e, ".image-container  a", "href", "https", "www.auchan.pt");
            saveDataProduct(internalId, null, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
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
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".search-result-count", true, 0);

      this.log("Total de produtos: " + this.totalProducts);
   }

}
