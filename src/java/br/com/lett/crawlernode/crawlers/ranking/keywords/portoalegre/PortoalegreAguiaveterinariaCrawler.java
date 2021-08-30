package br.com.lett.crawlernode.crawlers.ranking.keywords.portoalegre;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Collections;

public class PortoalegreAguiaveterinariaCrawler extends CrawlerRankingKeywords {

   public PortoalegreAguiaveterinariaCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 24;
      this.log("Página " + this.currentPage);

      String url = "https://www.aguiaveterinaria.com.br/busca?busca=" + this.keywordEncoded + "&pagina=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".row .fbits-item-lista-spot");

      if (!products.isEmpty()) {

         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String productUrl = CrawlerUtils.scrapUrl(e, ".spotContent a ", Collections.singletonList("href"), "https", "www.aguiaveterinaria.com.br");
            String internalPid = CommonMethods.getLast(productUrl.split("-"));
            saveDataProduct(null, internalPid, productUrl);

            this.log(
               "Position: " + this.position +
                  " - InternalId: " + null +
                  " - InternalPid: " + internalPid +
                  " - Url: " + productUrl);

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

   @Override
   protected boolean hasNextPage() {
      String nextPage = CrawlerUtils.scrapStringSimpleInfo(this.currentDoc, ".fbits-paginacao li:last-child a", true);
      return nextPage != null && nextPage.equals(">");
   }
}
