package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;

public class BrasilBelezanawebCrawler extends CrawlerRankingKeywords {

   public BrasilBelezanawebCrawler(Session session) {
      super(session);
   }

   @Override
   protected Document fetchDocument(String url) {
      try {
         HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
         HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(url))
            .build();
         HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
         return Jsoup.parse(response.body());
      } catch (Exception e) {
         throw new RuntimeException("Failed In load page: " + url, e);
      }
   }


   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 36;
      this.log("Página " + this.currentPage);

      String url = "https://www.belezanaweb.com.br/busca?q=" + this.keywordEncoded + "&pagina=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".showcase-item");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }
         for (Element e : products) {
            JSONObject json = CrawlerUtils.stringToJson(e.attr("data-event"));
            String internalId = json.optString("sku", null);
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "a.showcase-item-title", "href");
            String name = json.optString("productName");
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".showcase-image", Collections.singletonList("data-src"), "https", "res.cloudinary.com");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".price-value", null, false, ',', session, 0);
            boolean isAvailable = price != 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(null)
               .setImageUrl(imgUrl)
               .setName(name)
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

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected void setTotalProducts() {
      Element totalElement = this.currentDoc.selectFirst(".pagination-total strong");

      if (totalElement != null) {
         String total = totalElement.ownText().replaceAll("[^0-9]", "").trim();

         if (!total.isEmpty()) {
            this.totalProducts = Integer.parseInt(total);
         }
      }

      this.log("Total da busca: " + this.totalProducts);
   }
}
