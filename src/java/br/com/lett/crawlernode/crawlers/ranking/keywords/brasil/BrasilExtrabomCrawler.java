package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilExtrabomCrawler extends CrawlerRankingKeywords {


   public BrasilExtrabomCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 20;
      this.log("Página " + this.currentPage);

      String url = "https://www.extrabom.com.br/busca/?q=" + this.keywordEncoded + "&page=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".carousel__detalhes-top");

      if (!products.isEmpty()) {

         for (Element e : products) {
            String data = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "a", "href");

            String urlProduct = "";
            String internalId = "";

            if (data != null) {
               urlProduct = "https://www.taqi.com.br" + data;
               internalId = data.split("/")[2];
            }

            saveDataProduct(internalId, null, urlProduct);

            this.log(
               "Position: " + this.position +
                  " - InternalId: " + internalId +
                  " - InternalPid: " + null +
                  " - Url: " + urlProduct);

            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
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
      Element pagination = this.currentDoc.selectFirst(".pagination-box a:nth-last-child(2) > span");
      String selector = pagination.text();
      return selector != null && selector.contains("»");
   }
}
