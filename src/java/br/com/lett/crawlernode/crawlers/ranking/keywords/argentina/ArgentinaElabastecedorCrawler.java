package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ArgentinaElabastecedorCrawler extends CrawlerRankingKeywords {

   protected String getPHPSESSID() {
      return session.getOptions().optString("PHPSESSID", "eebitnngt7urf6tat1ompvsr9p");
   }

   public ArgentinaElabastecedorCrawler(Session session) {
      super(session);
   }

   private Map<String, String> getHeaders() {
      Map<String, String> headers = new HashMap<>();
      headers.put("Connection", "gzip, deflate, br");
      headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
      headers.put("referer", "https://www.elabastecedor.com.ar/index.php");
      headers.put("authority", "www.elabastecedor.com.ar");
      headers.put("cookie", "PHPSESSID=" + getPHPSESSID() + ";");
      return headers;
   }

   @Override
   protected Document fetchDocument(String url) {
      Map<String, String> headers1 = getHeaders();
      headers1.put("Content-Type", "application/x-www-form-urlencoded");
      headers1.put("origin", "https://www.elabastecedor.com.ar/resultado-temp.php?search=leche");
      String payload = "search=" + this.keywordEncoded;

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setPayload(payload)
         .setHeaders(headers1)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_ROTATE_AR
         ))
         .build();

      Response response = new FetcherDataFetcher().post(session, request);

      Request request2 = Request.RequestBuilder.create()
         .setUrl("https://www.elabastecedor.com.ar/busqueda-resultado")
         .setHeaders(getHeaders())
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_ROTATE_AR
         ))
         .build();

      Response response2 = new FetcherDataFetcher().get(session, request2);

      return Jsoup.parse(response2.getBody());
   }


   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 20;

      this.log("Página " + this.currentPage);

      String url = "https://www.elabastecedor.com.ar/busqueda";
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select("article.list-product");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) setTotalProducts();
         for (Element e : products) {
            String productUrl = crawlProductUrl(e);
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".produItem", "data-id");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".nombreProducto span", false);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".img-block img.second-img", Arrays.asList("src"), "https", "www.elabastecedor.com.ar");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".current-price", null, false, ',', session, null);
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalId)
               .setName(name)
               .setImageUrl(imgUrl)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);

            if (this.arrayProducts.size() == productsLimit) break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

   }

   private String crawlProductUrl(Element e) {
      String url = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".inner-link ", "href");
      return CrawlerUtils.completeUrl(url, "https:", "www.elabastecedor.com.ar");
   }

   @Override
   protected boolean hasNextPage() {
      return false;
   }
}
