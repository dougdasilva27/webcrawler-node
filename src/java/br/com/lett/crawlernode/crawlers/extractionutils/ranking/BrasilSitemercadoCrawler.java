package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import java.util.HashMap;
import java.util.Map;

import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

public abstract class BrasilSitemercadoCrawler extends CrawlerRankingKeywords {

   public BrasilSitemercadoCrawler(Session session) {
      super(session);
   }

   private String homePage = getHomePage();
   private String loadPayload = getLoadPayload();

   protected abstract String getHomePage();

   protected String getLoadPayload() {
      JSONObject payload = new JSONObject();
      String[] split = homePage.split("/");

      payload.put("lojaUrl", CommonMethods.getLast(split));
      payload.put("redeUrl", split[split.length - 2]);

      return payload.toString();
   }

   protected String getApiSearchUrl() {
      return "https://b2c-sm-www-api-production4.sitemercado.com.br/api/v1/b2c/380/product/load_search/";
   }

   private String ApiSearchUrl(String lojaId) {
      return "https://b2c-sm-www-api.sitemercado.com.br/api/v1/b2c/" + lojaId + "/product/load_search/";
   }

   @Override
   public void extractProductsFromCurrentPage() {
      this.log("Página " + this.currentPage);
      JSONObject search = crawlProductInfo();

      if (search.has("products") && search.getJSONArray("products").length() > 0) {
         JSONArray products = search.getJSONArray("products");

         this.totalProducts = products.length();
         this.log("Total da busca: " + this.totalProducts);

         for (int i = 0; i < products.length(); i++) {
            JSONObject product = products.getJSONObject(i);

            String productUrl = crawlProductUrl(product);
            String internalPid = crawlInternalPid(product);

            saveDataProduct(null, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);

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
      return false;
   }

   private String crawlInternalPid(JSONObject product) {
      String internalPid = null;

      if (product.has("idProduct")) {
         internalPid = product.get("idProduct").toString();
      }

      return internalPid;
   }

   private String crawlProductUrl(JSONObject product) {
      String productUrl = null;

      if (product.has("url")) {
         productUrl = product.getString("url");

         if (!productUrl.contains("sitemercado")) {
            productUrl = (this.homePage + "/" + productUrl).replace("//produto/", "/produto/");

            // This can be "tirandentes/" or "stamatis/"
            String lastUrlPart = CommonMethods.getLast(this.homePage.split("-"));

            if (productUrl.contains(lastUrlPart + "/") && !productUrl.contains("produto")) {
               productUrl = productUrl.replace(lastUrlPart + "/", lastUrlPart + "/produto/");
            }
         }
      }

      return productUrl;
   }

   private JSONObject crawlProductInfo() {
      String lojaUrl = CommonMethods.getLast(getHomePage().split("sitemercado.com.br"));
      String loadUrl = "https://b2c-sm-www-api.sitemercado.com.br/api/v1/b2c/page/store" + lojaUrl;
      String lojaId = "";
      String lojaRede = "";

      Map<String, String> headers = new HashMap<>();
      headers.put("referer", this.homePage);
      headers.put("accept", "application/json, text/plain, */*");
      headers.put("content-type", "application/json");

      Request request = RequestBuilder.create()
         .setUrl(loadUrl).setCookies(cookies)
         .setHeaders(headers)
         .setPayload(loadPayload)
         .build();
      Response response = this.dataFetcher.get(session, request);

      Map<String, String> responseHeaders = response.getHeaders();

      if (responseHeaders.containsKey("sm-token")) {
         String header = responseHeaders.get("sm-token");
         JSONObject token = new JSONObject(header);
         lojaId = Integer.toString(JSONUtils.getIntegerValueFromJSON(token, "IdLoja", 0));

         if(lojaId.equals("0")){
            JSONObject body = new JSONObject(response.getBody());
            lojaId =Integer.toString(JSONUtils.getValueRecursive(body,"sale.id",Integer.class));
            lojaRede = Integer.toString(JSONUtils.getValueRecursive(body,"sale.idRede",Integer.class));

            token.put("IdLoja",lojaId);
            token.put("IdRede",lojaRede);
         }

         headers.put("sm-token", token.toString());

      }


      String apiUrl = ApiSearchUrl(lojaId) + this.keywordEncoded;
      Request requestApi = RequestBuilder.create()
         .setUrl(apiUrl)
         .setCookies(cookies)
         .setHeaders(headers)
         .build();

      return CrawlerUtils.stringToJson(this.dataFetcher.get(session, requestApi).getBody());
   }

}
