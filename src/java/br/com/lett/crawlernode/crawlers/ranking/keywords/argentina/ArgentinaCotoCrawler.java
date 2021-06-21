package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import com.google.common.collect.Maps;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Map;

import static br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;

public class ArgentinaCotoCrawler extends CrawlerRankingKeywords {

   private String nextUrl;

   public ArgentinaCotoCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
      this.pageSize = 48;
   }

   @Override
   protected void extractProductsFromCurrentPage() {
//      cookies = fetchCookies("https://www.cotodigital3.com.ar/sitios/cdigi/home");
      this.log("Página " + this.currentPage);
      if (currentPage == 1) {
         JSONObject jsonObject = fetchUrl();
         String urlDoc = "https://www.cotodigital3.com.ar/sitios/cdigi/browse" + jsonObject.optQuery("/canonicalLink/navigationState");
         this.currentDoc = fetchDocument(urlDoc);
      }

      if (null != nextUrl) {
         this.currentDoc = fetchDocument(nextUrl);
      }
      Elements products = this.currentDoc.select("#products > li");
      pageSize = products.size();
      //se obter 1 ou mais links de produtos e essa página tiver resultado faça:
      if (products.size() >= 1) {
         //se o total de busca não foi setado ainda, chama a função para setar
         if (this.totalProducts == 0) setTotalProducts();
         for (Element e : products) {

            // InternalPid
            String internalPid = crawlInternalPid(e);

            // InternalId
            String internalId = crawlInternalId(e);

            // Url do produto
            String productUrl = crawlProductUrl(e);

            saveDataProduct(internalId, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
         }
      }
      nextUrl = "https://www.cotodigital3.com.ar/" + currentDoc.selectFirst("#atg_store_pagination li:nth-child(" + (currentPage + 1) + ") a").attr("href");
      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private JSONObject fetchUrl() {
      Map<String, String> headers = Maps.newHashMap();
      headers.put("Content-Type", "application/x-www-form-urlencoded");
      headers.put("Accept", " application/json");
      headers.put("Accept-Encoding", " gzip, deflate, br");
      String url = "https://www.cotodigital3.com.ar/sitios/cdigi/assembler?format=json&Dy=1&assemblerContentCollection=/content/Shared/Auto-Suggest%20Panels&Ntt=" + keywordEncoded;
      Request request = RequestBuilder.create().setCookies(cookies).setHeaders(headers).setUrl(url).build();
      Response response = new ApacheDataFetcher().get(session, request);

      return CrawlerUtils.stringToJson(response.getBody());
   }

   @Override
   protected boolean hasNextPage() {
      return this.arrayProducts.size() < this.totalProducts;
   }

   @Override
   protected void setTotalProducts() {
      Element totalElement = this.currentDoc.select("#resultsCount").first();

      if (totalElement != null) {
         try {
            this.totalProducts = Integer.parseInt(totalElement.text().replaceAll("[^0-9]", "").trim());
         } catch (Exception e) {
            this.logError(CommonMethods.getStackTrace(e));
         }

         this.log("Total da busca: " + this.totalProducts);
      }
   }

   private String crawlInternalId(Element e) {
      String internalId = null;

      String url = e.attr("href");
      if (url.contains("/p/")) {
         String[] tokens = url.split("/");
         internalId = tokens[tokens.length - 1].replaceAll("[^0-9]", "");
      }

      return internalId;
   }

   private String crawlInternalPid(Element e) {

      return e.attr("id").replaceAll("[^0-9]", "").trim();
   }

   private String crawlProductUrl(Element e) {
      String productUrl = null;
      Element eUrl = e.select(".product_info_container > a").first();

      if (eUrl != null) {
         productUrl = "https://www.cotodigital3.com.ar" + eUrl.attr("href");
      }

      return productUrl;
   }
}
