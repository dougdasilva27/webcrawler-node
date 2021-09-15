package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import org.jboss.marshalling.ObjectTable;
import org.json.JSONArray;
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
      Document doc = new Document(url);
      int attempts = 0;
      Map<String, String> headers = new HashMap<>();
      headers.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.159 Safari/537.36");

      do {
         Request request = Request.RequestBuilder.create()
            .setUrl(url)
            .setProxyservice(Arrays.asList(
               ProxyCollection.NETNUT_RESIDENTIAL_DE_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY
            ))
            .setHeaders(headers)
            .build();

         Response response = new JsoupDataFetcher().get(session, request);
         doc = Jsoup.parse(response.getBody());
         attempts++;

         if (attempts == 3) {
            if (isBlockedPage(doc)) {
               Logging.printLogInfo(logger, session, "Blocked after 3 retries.");
            }
            break;
         }
      }
      while(isBlockedPage(doc));

      return doc;
   }

   private boolean isBlockedPage(Document doc) {
      return doc.toString().contains("We are sorry") || doc.selectFirst("div") == null;
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 60;
      this.log("Página " + this.currentPage);

      String url = "https://www.magazineluiza.com.br/busca/" + keywordEncoded + "?page=" + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);
      JSONObject json = CrawlerUtils.selectJsonFromHtml(this.currentDoc, "script#__NEXT_DATA__", null, null, false, false);

      JSONArray products = JSONUtils.getValueRecursive(json, "props.pageProps.data.search.products", JSONArray.class);
      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            this.totalProducts = JSONUtils.getValueRecursive(json, "props.pageProps.data.search.pagination.records", Integer.class);
            this.log("Total da busca: " + this.totalProducts);
         }

         for (Object e : products) {
            JSONObject product = (JSONObject) e;

            String internalId = product.optString("variationId");
            String urlProduct = "https://www.magazineluiza.com.br/" + product.optString("url");

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
}
