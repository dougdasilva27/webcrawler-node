package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SavegnagoRanking extends CrawlerRankingKeywords {

   private String storeId;

   //This token never changes. BUT, if necessary, we can get the token using the 'getAppToken' method
   protected String APP_TOKEN = "DWEYGZH2K4M5N7Q8R9TBUCVEXFYG2J3K4N6P7Q8SATBUDWEXFZH2J3M5N6";

   public SavegnagoRanking(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
      storeId = session.getOptions().optString("cityCode");
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
         .setSendUserAgent(true)
         .build();

      String response = CrawlerUtils.retryRequestString(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher(), new FetcherDataFetcher()), session);

      return CrawlerUtils.stringToJson(response);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 30;
      this.log("Página " + this.currentPage);

      String url = "https://api.savegnago.com.br/search?input=" + this.keywordEncoded + "&page=" + this.currentPage + "&salesChannel=" + storeId;

      JSONObject json = fetchJSONObject(url);
      JSONArray products = json.optJSONArray("products");

      if (products != null && !products.isEmpty()) {
         if (totalProducts == 0) {
            setTotalProducts(json);
         }

         for (Object obj : products) {
            if (obj instanceof JSONObject) {
               JSONObject product = (JSONObject) obj;

               String internalPid = product.optString("id");
               String name = product.optString("name");
               String productUrl = buildUrl(name, internalPid);
               Integer price = (int) Math.round((product.optDouble("price") * 100));
               String imgUrl = JSONUtils.getValueRecursive(product, "images.url", String.class);
               boolean isAvailable = product.optString("status").equals("AVAILABLE") ? true : false;

               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(null)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setImageUrl(imgUrl)
                  .setPriceInCents(price)
                  .setAvailability(isAvailable)
                  .build();

               saveDataProduct(productRanking);
               
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
