package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class BrasilMagazineluizaCrawler extends CrawlerRankingKeywords {

   public BrasilMagazineluizaCrawler(Session session) {
      super(session);
   }

   @Override
   protected Document fetchDocument(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.159 Safari/537.36");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build();

      Response response = this.dataFetcher.get(session, request);

      return Jsoup.parse(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 60;
      this.log("Página " + this.currentPage);

      String url = "https://www.magazineluiza.com.br/busca/" + keywordEncoded + "/" + this.currentPage +"/";
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".productShowCase.big li");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {

            String jsonString = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "a[data-product]", "data-product");
            JSONObject productJson = CrawlerUtils.stringToJson(jsonString);

            String internalId = productJson.optString("basketId");
            String urlProduct = CrawlerUtils.scrapUrl(e, ".product a", "href", "https", "www.magazineluiza.com.br");

            saveDataProduct(internalId, null, urlProduct);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + urlProduct);
            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      if (!hasNextPage() && this.arrayProducts.size() > this.totalProducts) {
         this.totalProducts = this.arrayProducts.size();
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

   }

   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(currentDoc, "[itemprop='description'] small", null, null, false, true, 0);
      this.log("Total da busca: " + this.totalProducts);
   }
}
