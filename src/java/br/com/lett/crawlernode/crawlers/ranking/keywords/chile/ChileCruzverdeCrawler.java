package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.HttpHeaders;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChileCruzverdeCrawler extends CrawlerRankingKeywords {

   public ChileCruzverdeCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   private final int STORE_ID = session.getOptions().optInt("store_id", 1121);

   private Map<String, String> getHeaders() {
      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.CONTENT_TYPE, "application/json");
      headers.put("origin", "https://cruzverde.cl");
      headers.put("authority", "api.cruzverde.cl");
      headers.put("referer", "https://cruzverde.cl/");
      headers.put("accept", "application/json, text/plain, */*");
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");

      return headers;
   }


   @Override
   protected void processBeforeFetch() {

      Request request = Request.RequestBuilder.create()
         .setUrl("https://api.cruzverde.cl/customer-service/login")
         .setHeaders(getHeaders())
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_ES,
            ProxyCollection.NETNUT_RESIDENTIAL_BR
         ))
         .setSendUserAgent(true)
         .setPayload("")
         .setFetcheroptions(FetcherOptions.FetcherOptionsBuilder.create().mustUseMovingAverage(false).mustRetrieveStatistics(true).build())
         .build();

      Response responseApi = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher(), new FetcherDataFetcher()), session, "post");
      ;

      this.cookies.addAll(responseApi.getCookies());
   }


   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 12;
      this.log("Página " + this.currentPage);
      JSONObject reponseJson = getProductList();

      if (!reponseJson.isEmpty()) {
         this.totalProducts = reponseJson.optInt("total");
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         JSONArray productsList = reponseJson.getJSONArray("hits");

         for (Object arrayOfArrays : productsList) {

            JSONObject jsonInfo = (JSONObject) arrayOfArrays;
            String internalId = getInternalId(jsonInfo);
            JSONObject extractAvaliPrice = getPrice(internalId);
            String productUrl = getProductUrl(jsonInfo);
            String name = getName(jsonInfo);
            String imgUrl = getImg(jsonInfo);
            boolean isAvailable = getAvaliability(extractAvaliPrice, internalId);
            Integer price = null;
            if (isAvailable) {
               price = getPrice(extractAvaliPrice, internalId);
            }

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(null)
               .setImageUrl(imgUrl)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);
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

   private JSONObject getProductList() {
      Map<String, String> headers = getHeaders();
      headers.put("cookie", CommonMethods.cookiesToString(this.cookies));

      String url = "https://api.cruzverde.cl/product-service/products/search?limit=" + pageSize + "&offset=" + this.arrayProducts.size() + "&sort=&q=" + keywordEncoded;

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .mustSendContentEncoding(true)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_ES,
            ProxyCollection.NETNUT_RESIDENTIAL_BR
         ))
         .build();

      String response = CrawlerUtils.retryRequestString(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher(), new FetcherDataFetcher()), session);

      if (response != null) {
         return CrawlerUtils.stringToJson(response);
      }

      return null;
   }

   private String getInternalId(JSONObject prodList) {
      return prodList.optString("productId");
   }

   private Integer getPrice(JSONObject obj, String id) {
      Object stockPrice = obj.optQuery("/" + id + "/prices/price-sale-cl");
      if (stockPrice == null) {
         stockPrice = obj.optQuery("/" + id + "/prices/price-list-cl");
      }
      return Integer.parseInt(stockPrice.toString());
   }

   private boolean getAvaliability(JSONObject obj, String id) {

      Object stock = obj.optQuery("/" + id + "/stock");
      return stock != null;
   }

   private JSONObject getPrice(String id) {
      Map<String, String> headers = getHeaders();
      headers.put("cookie", CommonMethods.cookiesToString(this.cookies));

      JSONObject obj = null;
      String url = "https://api.cruzverde.cl/product-service/products/product-summary?ids=" + id + "&ids=" + id + "&fields=stock&fields=prices&fields=promotions&inventoryId=" + STORE_ID;
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .mustSendContentEncoding(true)
         .setHeaders(headers)
         .build();
      String response = CrawlerUtils.retryRequestString(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher(), new FetcherDataFetcher()), session);
      if (response != null) {
         obj = CrawlerUtils.stringToJson(response);
      }

      return obj;
   }

   private String getImg(JSONObject prodList) {
      String img = "";
      Object objImg = prodList.optQuery("/image/link");
      if (objImg != null) {
         img = objImg.toString();
      }
      return img;

   }

   private String getName(JSONObject prodList) {
      String name = "";
      Object objName = prodList.optString("productName");
      if (objName != null) {
         name = objName.toString();
      }
      return name;
   }

   private String getProductUrl(JSONObject prodList) {
      String objname = prodList.optString("productName");
      String objId = prodList.optString("productId");
      String replaceName = objname.replace(" ", "").replace("%", "").replace(",", "");
      return "https://www.cruzverde.cl/" + replaceName + "/" + objId + ".html";
   }

}

