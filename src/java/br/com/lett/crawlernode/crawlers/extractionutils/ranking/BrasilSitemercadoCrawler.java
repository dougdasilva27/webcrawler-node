package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class BrasilSitemercadoCrawler extends CrawlerRankingKeywords {

   public BrasilSitemercadoCrawler(Session session) {
      super(session);
   }

   private final String API_URL = getApiUrl();
   private String homePage = getHomePage();
   private String loadPayload = getLoadPayload();

   protected String getHomePage() {
      return session.getOptions().optString("url");
   }

   protected String getApiUrl(){
      return "https://www.sitemercado.com.br/api/v1/b2c/";
   }


   protected String getLoadPayload() {
      JSONObject payload = new JSONObject();
      String[] split = homePage.split("/");

      payload.put("lojaUrl", CommonMethods.getLast(split));
      payload.put("redeUrl", split[split.length - 2]);

      return payload.toString();
   }


   protected String apiSearchUrl(String lojaId) {
      return API_URL + lojaId + "/product/load_search/";
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

   protected JSONObject crawlProductInfo() {
      String lojaUrl = CommonMethods.getLast(getHomePage().split("sitemercado.com.br"));
      String loadUrl = API_URL+"page/store" + lojaUrl;
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


      String apiUrl = apiSearchUrl(lojaId) + this.keywordEncoded.replace("+", "%20");
      Request requestApi = RequestBuilder.create()
         .setUrl(apiUrl)
         .setCookies(cookies)
         .setHeaders(headers)
         .build();

      return CrawlerUtils.stringToJson(this.dataFetcher.get(session, requestApi).getBody());
   }

}
