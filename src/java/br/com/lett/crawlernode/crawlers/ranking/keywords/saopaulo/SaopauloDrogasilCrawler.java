package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import java.util.HashMap;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class SaopauloDrogasilCrawler extends CrawlerRankingKeywords {

   public SaopauloDrogasilCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 12;

      this.log("Página " + this.currentPage);
      String keyword = this.keywordWithoutAccents.replace(" ", "%20");

      this.log("Página " + this.currentPage);

      // monta a url com a keyword e a página
      String url = "https://busca.drogasil.com.br/search?w=" + keyword + "&cnt=36&srt="
            + this.arrayProducts.size();

      Map<String, String> headers = new HashMap<>();
      headers.put("upgrade-insecure-requests", "1");
      headers.put("accept", "text/html");
      headers.put("accept-language", "pt-BR");

      this.log("Link onde são feitos os crawlers: " + url);

      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).build();
      String response = new FetcherDataFetcher().get(session, request).getBody();

      if (response != null && !response.isEmpty()) {
         this.currentDoc = Jsoup.parse(response);
      } else {
         this.currentDoc = fetchDocument(url);
      }


      Elements products = this.currentDoc.select(".item div.container:not(.min-limit)");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element e : products) {
            String internalId = crawlInternalId(e);
            String productUrl = crawlProductUrl(e);

            saveDataProduct(internalId, null, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
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
      Element totalElement = this.currentDoc.selectFirst("p.amount");

      if (totalElement != null && this.currentDoc.selectFirst("#pantene") == null) {
         String token = totalElement.text().replaceAll("[^0-9]", "").trim();

         if (!token.isEmpty()) {
            this.totalProducts = Integer.parseInt(token);
         }

         this.log("Total da busca: " + this.totalProducts);
      }
   }

   private String crawlInternalId(Element e) {
      String internalId = null;
      Element id = e.selectFirst(".trustvox-shelf-container[data-trustvox-product-code]");

      if (id != null) {
         internalId = id.attr("data-trustvox-product-code");
      }

      return internalId;
   }

   private String crawlProductUrl(Element e) {
      String urlProduct = null;
      Element urlElement = e.selectFirst(".product-name.sli_title a");

      if (urlElement != null) {
         urlProduct = urlElement.attr("title");
      } else {
         urlElement = e.selectFirst(".product-name a");

         if (urlElement != null) {

            urlProduct = CrawlerUtils.completeUrl(urlElement.attr("href"), "https://", "www.drogasil.com.br");

         }
      }

      return urlProduct;
   }

}
