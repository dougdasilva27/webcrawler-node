package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.JSONUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SavegnagoRanking extends CrawlerRankingKeywords {

   private static final String BASE_URL = "www.savegnago.com.br";
   private String urlModel;
   private final String storeId = getStoreId();
   private final String salesChannel = getSalesChannel();

   //This token never changes. BUT, if necessary, we can get the token using the 'getAppToken' method
   protected String APP_TOKEN = "DWEYGZH2K4M5N7Q8R9TBUCVEXFYG2J3K4N6P7Q8SATBUDWEXFZH2J3M5N6";

   public String getSalesChannel() {
      return salesChannel;
   }

   public String getStoreId() {
      return storeId;
   }

   public SavegnagoRanking(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   //If the app token changes, we can use this method to get it
   protected void getAppToken() {
      String url = "https://savegnago.com.br/_next/static/chunks/950816df62fa2d85fc3ac8e13cd05335a6db61c7.1e900a1ca2420496572b.js";

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .build();

      String jsResponse = this.dataFetcher.get(session, request).getBody();
      Pattern regexAppToken = Pattern.compile("APP_TOKEN:\\\"(.*?)\\\"");

      Matcher matcher = regexAppToken.matcher(jsResponse);

      if (matcher.find()) {
         APP_TOKEN = matcher.group(1);
      }
   }

   @Override
   protected JSONObject fetchJSONObject(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("app-token", APP_TOKEN);
      headers.put("app-key", "betaappkey-savegnago-desktop");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setCookies(cookies)
         .build();

      String json = this.dataFetcher.get(session, request).getBody();

      return JSONUtils.stringToJson(json);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 30;
      this.log("Página " + this.currentPage);

      String url = "https://api.savegnago.com.br/search?input=" + this.keywordEncoded + "&page=" + this.currentPage + "&salesChannel=" + salesChannel;

      JSONObject json = fetchJSONObject(url);
      JSONArray products = json.optJSONArray("products");

      if (!products.isEmpty()) {
         if (totalProducts == 0) {
            setTotalProducts(json);
         }

         for (Object obj : products) {
            if (obj instanceof JSONObject) {
               JSONObject product = (JSONObject) obj;
               String internalPid = product.optString("id");
               String name = product.optString("name");
               String productUrl = buildUrl(name, internalPid);
               saveDataProduct(null, internalPid, productUrl);

               log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);
               if (arrayProducts.size() == productsLimit) {
                  break;
               }
            }
         }
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

   }

   protected void setTotalProducts(JSONObject json) {
      JSONObject paginationArray = json.optJSONObject("pagination");
      this.totalProducts = paginationArray.optInt("size");
      this.log("Total de produtos: " + this.totalProducts);
   }

   protected String buildUrl(String name, String internalPid){
      String url = "https://savegnago.com.br/produto/";
      url += StringUtils.stripAccents(name.toLowerCase()).replace(" ", "-");
      url += "/" + internalPid;
      return CommonMethods.sanitizeUrl(url);
   }

}
