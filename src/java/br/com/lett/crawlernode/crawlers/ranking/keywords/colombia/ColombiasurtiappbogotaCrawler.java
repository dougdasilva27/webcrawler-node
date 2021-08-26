package br.com.lett.crawlernode.crawlers.ranking.keywords.colombia;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ColombiasurtiappbogotaCrawler extends CrawlerRankingKeywords {

   @Override
   protected Document fetchDocument(String url) {

      Map<String, String> headers = new HashMap<>();
      headers.put("Cookie", session.getOptions().optString("cookie"));
      Request request = Request.RequestBuilder.create().setHeaders(headers).setUrl(url).build();
      Response response = dataFetcher.get(session, request);

      String html = response.getBody();

      return Jsoup.parse(html);
   }


   public ColombiasurtiappbogotaCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 25;
      this.log("Página " + this.currentPage);

      String url = "https://tienda.surtiapp.com.co/Store/SearchResults/" + this.keywordEncoded;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".product-card.product-id-contaniner");

      if (!products.isEmpty()) {
         for (Element e : products) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, null, "data-product");
            String productUrl = CrawlerUtils.completeUrl(internalId, "https", "tienda.surtiapp.com.co/Store/ProductDetail/");
            saveDataProduct(internalId, null, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null
               + " - Url: " + productUrl);

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

}
