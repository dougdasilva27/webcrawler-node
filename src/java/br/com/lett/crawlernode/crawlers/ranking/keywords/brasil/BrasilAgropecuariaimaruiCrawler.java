package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilAgropecuariaimaruiCrawler extends CrawlerRankingKeywords {

   public BrasilAgropecuariaimaruiCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 35;
      this.log("Página " + this.currentPage);

      String url = "https://agropecuariaimarui.com.br/page/" + this.currentPage + "/?s="+ this.keywordEncoded + "&post_type=product";

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".products .col-inner");

      if (!products.isEmpty()) {

         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, " .box-text .add-to-cart-button a", "data-product_id");
            String internalPid = crawlInternalPid(e);
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".image-none a", "href");

            saveDataProduct(internalId, internalPid, productUrl);

            this.log(
               "Position: " + this.position +
                  " - InternalId: " + internalId +
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

   private String crawlInternalPid(Element e) {
      String internalPid = null;

      if (e != null && e.hasAttr("data-gtm4wp_product_id")) {
         internalPid = e.attr("data-gtm4wp_product_id").trim();
      }

      return internalPid;
   }

   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(
         this.currentDoc,
         ".page-title-inner .flex-col p.woocommerce-result-count",
         "de",
         "resultados",
         false, true, 0);

      this.log("Total de produtos: " + this.totalProducts);
   }
}
