package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

import java.util.Iterator;

public class BrasilLojacomerbemCrawler extends CrawlerRankingKeywords {

   public BrasilLojacomerbemCrawler(Session session) {
      super(session);
   }

   @Override
   protected void processBeforeFetch() {
      JSONObject cookieJson = session.getOptions().optJSONObject("cookies");
      if (cookieJson != null) {
         for (
            Iterator<String> iter = cookieJson.keys(); iter.hasNext(); ) {
            String key = iter.next();
            BasicClientCookie cookie = new BasicClientCookie(key, cookieJson.optString(key));
            this.cookies.add(cookie);
         }

      }
   }


   @Override
   protected void extractProductsFromCurrentPage() {
      this.log("Página " + this.currentPage);

      this.pageSize = 24;

      String keyword = this.keywordWithoutAccents.replace(" ", "%20");
      String url = "https://www.lojacomerbem.com.br/" + keyword + "?PageNumber=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url, cookies);

      Elements products = this.currentDoc.select("li[layout] span[id]");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0)
            setTotalProducts();

         for (Element e : products) {
            String internalPid = e.id();
            String productUrl = CrawlerUtils.scrapUrl(e, ".product-name a", "href", "https", "www.lojacomerbem.com.br");

            saveDataProduct(null, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);
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
      Element totalElement = this.currentDoc.selectFirst(".resultado-busca-numero .value");

      if (totalElement != null) {
         String text = totalElement.text().replaceAll("[^0-9]", "").trim();

         if (!text.isEmpty()) {
            this.totalProducts = Integer.parseInt(text);
         }

         this.log("Total da busca: " + this.totalProducts);
      }
   }
}
