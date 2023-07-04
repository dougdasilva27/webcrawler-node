package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;


import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.JSONUtils;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

public class BrasilSvicenteCrawler extends CrawlerRankingKeywords {
   public BrasilSvicenteCrawler(Session session) {
      super(session);
   }

   private static final String HOME_PAGE = "https://www.svicente.com.br";

   protected JSONObject getProductList(String url) {
      String store = session.getOptions().optString("store");
      HashMap<String, String> headers = new HashMap<>();
      headers.put("cookie", "dwsid=2hVbN9KSztunRT1mShk_x-zLSWN80Q9ZLHPdOF69FWVh3PISHeZRi_lCamtzPeOew4RKeR_7GWAm4BfpiG9qUg==; hasSelectedStore=" + store + ";dw_store=" + store + ";");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build();
      Response response = new ApacheDataFetcher().get(session, request);
      if (response.getCookies() != null && !response.getCookies().isEmpty()) {
         for (Cookie cookie : response.getCookies()) {
            BasicClientCookie cookieAdd = new BasicClientCookie(cookie.getName(), cookie.getValue());
            cookieAdd.setDomain("www.svicente.com.br");
            cookieAdd.setPath("/");
            this.cookies.add(cookieAdd);
         }

      }
      return JSONUtils.stringToJson(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      int start = (this.currentPage - 1) * 24;
      String url = HOME_PAGE + "/on/demandware.store/Sites-SaoVicente-Site/pt_BR/Search-UpdateGrid?q=" + this.keywordEncoded + "&start=" + start + "&sz=24&selectedUrl";
      JSONObject responseJson = getProductList(url);


      if (!responseJson.isEmpty()) {

         if (this.totalProducts == 0) {
            this.totalProducts = JSONUtils.getValueRecursive(responseJson, "productSearch.count", Integer.class, 0);
         }

         JSONArray productsList = responseJson.getJSONArray("productsSearchResult");

         for (Object productObject : productsList) {
            if (productObject instanceof JSONObject) {
               JSONObject product = (JSONObject) productObject;
               String internalId = product.optString("id");
               String productUrl = product.optString("productShowFullUrl");
               String name = product.optString("productName");
               String imageUrl = JSONUtils.getValueRecursive(product, "images.medium.0.absURL", String.class, "").replaceAll(" ", "%20");
               Integer price = CommonMethods.stringPriceToIntegerPrice(JSONUtils.getValueRecursive(product, "price.sales.decimalPrice", String.class, null), '.', 0);
               boolean isAvailable = product.optBoolean("available");
               price = isAvailable ? price : null;


               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(internalId)
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
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }
      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

   }

}
