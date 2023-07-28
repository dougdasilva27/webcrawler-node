package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class BrasilDutramaquinasCrawler extends CrawlerRankingKeywords {

   public BrasilDutramaquinasCrawler(Session session) {
      super(session);
   }

   @Override
   protected Document fetchDocument(String url) {
      try {
         HttpClient client = HttpClient.newBuilder().build();
         HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(url))
            .build();
         HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
         return Jsoup.parse(response.body());
      } catch (Exception e) {
         throw new RuntimeException("Failed In load document: " + url, e);
      }
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 24;
      this.log("Página " + this.currentPage);
      String url = "https://www.dutramaquinas.com.br/model/md_busca_site.php?vc_termo=" + this.keywordEncoded + "&termo_tipo=simples&tot_termos=1&ordering=relevancia&max=" + this.pageSize + "&pg_num=" + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select("div > .produto.list");

      if (!products.isEmpty()) {

         for (Element e : products) {
            String internalId = e.attr("key");
            String urlProduct = CrawlerUtils.completeUrl(CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".produto-slick > a", "href"), "https", "www.dutramaquinas.com.br");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, "h3 > a", true);
            String imgUrl = CrawlerUtils.completeUrl(CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".produto-slick > a > img", "data-original"), "https", "www.dutramaquinas.com.br");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".valor-total", null, false, ',', session, null);
            boolean isAvailable = price != null;
            if (!isAvailable) {
               price = null;
            }

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(urlProduct)
               .setInternalId(internalId)
               .setImageUrl(imgUrl)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);
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
   protected boolean hasNextPage() {
      return true;
   }
}
