package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import org.apache.http.cookie.Cookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BrasilBifarmaCrawler extends CrawlerRankingKeywords {


   public BrasilBifarmaCrawler(Session session) {
      super(session);
   }

   @Override
   protected Document fetchDocument(String url, List<Cookie> cookies) {
      Map<String, String> headers = new HashMap<>();
      this.currentDoc = new Document(url);

      headers.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.63 Safari/537.36");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .mustSendContentEncoding(false)
         .setHeaders(headers)
         .setFetcheroptions(
            FetcherOptions.FetcherOptionsBuilder.create()
               .mustUseMovingAverage(false)
               .mustRetrieveStatistics(true)
               .setForbiddenCssSelector("script[src*=Incapsula]")
               .build()
         ).setProxyservice(
            Arrays.asList(
               ProxyCollection.BUY_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY
            )
         ).build();


      Response response = new ApacheDataFetcher().get(session, request);
      String content = response.getBody();

      int statusCode = response.getLastStatusCode();

      if ((Integer.toString(statusCode).charAt(0) != '2' &&
         Integer.toString(statusCode).charAt(0) != '3'
         && statusCode != 404)) {
         request.setProxyServices(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY));

         content = new FetcherDataFetcher().get(session, request).getBody();
      }

      return Jsoup.parse(content);

   }


   @Override
   protected void extractProductsFromCurrentPage() {
      this.log("Página " + this.currentPage);

      // monta a url com a keyword e a página
      String url = "https://www.bifarma.com.br/busca_Loja.html?q="
         + this.keywordWithoutAccents.replace(" ", "+");
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select("#gridProdutos .product");

      // se obter 1 ou mais links de produtos e essa página tiver resultado
      // faça:
      if (!products.isEmpty()) {
         for (Element e : products) {

            // InternalPid
            String internalPid = crawlInternalPid(e);

            // Url do produto
            String productUrl = crawlProductUrl(e);

            saveDataProduct(internalPid, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalPid + " - InternalPid: "
               + internalPid + " - Url: " + productUrl);
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
   protected boolean hasNextPage() {
      return false;
   }

   private String crawlInternalPid(Element e) {
      String internalPid = null;

      Element id = e.select("#produto_id").first();

      if (id != null) {
         internalPid = id.val();
      }

      return internalPid;
   }

   private String crawlProductUrl(Element e) {
      String productUrl = null;

      Element url = e.select("meta[itemprop=url]").first();

      if (url != null) {
         productUrl = url.attr("content");

         if (!productUrl.contains("bifarma")) {
            productUrl = "https://www.bifarma.com.br/" + productUrl;
         } else if (!productUrl.contains("http")) {
            productUrl = "https://" + productUrl;
         }
      }

      return productUrl;
   }
}
