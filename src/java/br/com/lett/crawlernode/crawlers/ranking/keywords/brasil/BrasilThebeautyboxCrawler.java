package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;

public class BrasilThebeautyboxCrawler extends CrawlerRankingKeywords {

   public BrasilThebeautyboxCrawler(Session session) {
      super(session);
   }

   @Override
   protected Document fetchDocument(String url) {
      try {
         HttpResponse<String> response = retryRequest(url, session);
         return Jsoup.parse(response.body());
      } catch (Exception e) {
         throw new RuntimeException("Failed In load page: " + url, e);
      }
   }

   public static HttpResponse retryRequest(String url, Session session) throws IOException, InterruptedException {
      HttpResponse<String> response = null;
      ArrayList<Integer> ipPort = new ArrayList<Integer>();
      ipPort.add(3132); //netnut br haproxy
      ipPort.add(3135); //buy haproxy
      ipPort.add(3133); //netnut ES haproxy
      ipPort.add(3138); //netnut AR haproxy

      try {
         for (int interable = 0; interable < ipPort.size(); interable++) {
            response = RequestHandler(url, ipPort.get(interable));
            if (response.statusCode() == 200) {
               return response;
            }
         }
      } catch (Exception e) {
         throw new RuntimeException("Failed In load document: " + session.getOriginalURL(), e);
      }
      return response;
   }

   private static HttpResponse RequestHandler(String url, Integer port) throws IOException, InterruptedException {
      HttpClient client = HttpClient.newBuilder().proxy(ProxySelector.of(new InetSocketAddress("haproxy.lett.global", port))).build();
      HttpRequest request = HttpRequest.newBuilder()
         .GET()
         .uri(URI.create(url))
         .build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      return response;
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 36;
      this.log("Página " + this.currentPage);

      String url = "https://www.beautybox.com.br/busca?q=" + this.keywordEncoded + "&pagina=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".showcase-gondola .showcase-item.js-event-search");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }
         for (Element e : products) {

            JSONObject data = JSONUtils.stringToJson(CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".showcase-item.js-event-search", "data-event"));

            String internalId = data != null ? data.optString("sku") : null;

            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".showcase-item-image", "href");

            String name = data.optString("productName");
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".showcase-item-image img", Collections.singletonList("data-src"), "", "");
            Double priceDouble = data.optDouble("price");
            Integer price = (int) Math.round(100 * priceDouble);

            Boolean isAvailable = price > 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setName(name)
               .setImageUrl(imgUrl)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);
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
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".pagination-total strong", null, null, false, true, 0);
      this.log("Total da busca: " + this.totalProducts);
   }

}
