package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;

public class BrasilAbcdaconstrucaoCrawler extends CrawlerRankingKeywords {

   public BrasilAbcdaconstrucaoCrawler(Session session) {
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
         throw new RuntimeException("Failed in load document: " + session.getOriginalURL(), e);
      }
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {

      this.pageSize = 12;
      this.log("Página " + this.currentPage);
      String url = "https://www.abcdaconstrucao.com.br/busca?busca=" + this.keywordWithoutAccents.replace(" ", "%20") + "&pagina=" + this.currentPage + "&tamanho=" + pageSize;
      if (keywordWithoutAccents.equalsIgnoreCase("portobello")) {
         url = "https://www.abcdaconstrucao.com.br/fabricante/portobello-2499290?pagina=" + this.currentPage + "&tamanho=24";
      }
      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".spot");

      if (!products.isEmpty()) {

         for (Element e : products) {
            String productUrl = CrawlerUtils.scrapUrl(e, ".spot__image-wrapper > a", "href", "https", "www.abcdaconstrucao.com.br").replaceAll("`","%60");
            String internalPid = CommonMethods.getLast(productUrl.split("-"));
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".spot__content-title > a > h3", true);
            String imageUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".spotImg img", Collections.singletonList("data-original"), "https", "www.abcdaconstrucao.com.br");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".precoPor > span", null, false, ',', session, null);
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalPid(internalPid)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
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
   protected boolean hasNextPage() {
      return !this.currentDoc.select("#next-page").isEmpty();
   }
}
