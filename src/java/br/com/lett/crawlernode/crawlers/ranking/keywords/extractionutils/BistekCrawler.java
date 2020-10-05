package br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;

public abstract class BistekCrawler extends CrawlerRankingKeywords {

   private final String HOST = getLocation() + ".bistekonline.com.br";

   public BistekCrawler(Session session) {
      super(session);
   }

   protected abstract String getLocation();

//   @Override
//   protected void processBeforeFetch() {
//      this.cookies = CrawlerUtils.fetchCookiesFromAPage("https://www.bistekonline.com.br/store/SetStoreByZipCode?zipCode=88066-000", null, HOST, "/",
//         cookies, session, new HashMap<>(), dataFetcher);
//   }

   @Override
   public void extractProductsFromCurrentPage() {
      this.pageSize = 12;
      this.log("Página " + this.currentPage);

      String url = "https://" + getLocation() + ".bistek.com.br/catalogsearch/result/index/?p=" + this.currentPage + "&q=" + this.keywordEncoded;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url, cookies);
      Elements products = this.currentDoc.select(".product-item");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }
         for (Element e : products) {
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".price-box", "data-product-id");
            String internalid = internalPid;
            String urlProduct = CrawlerUtils.scrapUrl(e, ".product-item-photo", "href", "https://", HOST);

            saveDataProduct(internalid, internalPid, urlProduct);

            this.log("Position: " + this.position + " - InternalId: " + internalid + " - InternalPid: " + internalPid + " - Url: " + urlProduct);
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
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(currentDoc, "#toolbar-amount > span:last-child", true, 0);
      this.log("Total da busca: " + this.totalProducts);
   }
}
