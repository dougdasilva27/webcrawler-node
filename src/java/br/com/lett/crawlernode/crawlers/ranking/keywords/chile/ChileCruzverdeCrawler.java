package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChileCruzverdeCrawler extends CrawlerRankingKeywords {

   public ChileCruzverdeCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.JSOUP;
   }

   @Override
   protected void processBeforeFetch() {
      Request request = Request.RequestBuilder.create()
         .setUrl("https://api.cruzverde.cl/customer-service/login")
         .setProxyservice(List.of(ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY, ProxyCollection.LUMINATI_SERVER_BR, ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY, ProxyCollection.BUY_HAPROXY))
         .build();
      Response responseApi = dataFetcher.post(session, request);
      String cookie = responseApi.getHeaders().toString();

      String finalCookie = null;
      String regex = "sid=(.*); Path";
      Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      final Matcher matcher = pattern.matcher(cookie);

      if (matcher.find()) {
         finalCookie = matcher.group(1);
      }

      BasicClientCookie sidCookie = new BasicClientCookie("connect.sid", finalCookie);
      sidCookie.setDomain("api.cruzverde.cl");
      sidCookie.setPath("/");
      sidCookie.setValue(finalCookie);
      sidCookie.setSecure(true);
      sidCookie.setAttribute("HttpOnly", "true");
      this.cookies.add(sidCookie);

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
            JSONObject extractAvaliPrice = getAvaliPrice(internalId);
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

      String url = "https://api.cruzverde.cl/product-service/products/search?limit=" + pageSize + "&offset=" + this.arrayProducts.size() + "&sort=&q=" + keywordEncoded;

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .mustSendContentEncoding(true)
         .setCookies(this.cookies)
         .setProxyservice(List.of(ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY, ProxyCollection.LUMINATI_SERVER_BR, ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY, ProxyCollection.BUY_HAPROXY))
         .build();
      Response response = this.dataFetcher.get(session, request);
      if (response != null) {
         return CrawlerUtils.stringToJson(response.getBody());
      }
      return null;
   }

   private String getInternalId(JSONObject prodList) {
      Object objId = prodList.optString("productId");
      return objId.toString();
   }

   private Integer getPrice(JSONObject obj, String id) {
      int price = 0;
      Object stockPrice = obj.optQuery("/" + id + "/prices/price-sale-cl");
      if (stockPrice == null){
         stockPrice = obj.optQuery("/" + id + "/prices/price-list-cl");
      }
      return Integer.parseInt(stockPrice.toString());
   }

   // Nova Request pra retornar o JSON que contem o stock e o preço
   private boolean getAvaliability(JSONObject obj, String id) {

      Object stock = obj.optQuery("/" + id + "/stock");
      int stockNumber = Integer.parseInt(stock.toString());
      return stockNumber > 0;

   }

   private JSONObject getAvaliPrice(String id) {
      JSONObject obj = null;
      String storeId = "1121";
      String url = "https://api.cruzverde.cl/product-service/products/product-summary?ids=" + id + "&ids=" + id + "&fields=stock&fields=prices&fields=promotions&inventoryId=" + storeId;
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .mustSendContentEncoding(true)
         .setCookies(this.cookies)
         .setProxyservice(List.of(ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY, ProxyCollection.LUMINATI_SERVER_BR, ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY, ProxyCollection.BUY_HAPROXY))
         .build();
      Response response = this.dataFetcher.get(session, request);
      if (response != null) {
         obj = CrawlerUtils.stringToJson(response.getBody());
      }

      return obj;
   }

   private String getImg(JSONObject prodList) {
      Object objImg = prodList.optQuery("/image/link").toString();
      return objImg.toString();

   }

   private String getName(JSONObject prodList) {
      Object objName = prodList.optString("productName");
      return objName.toString();
   }

   private String getProductUrl(JSONObject prodList) {
      Object objname = prodList.optString("productName");
      Object objId = prodList.optString("productId");
      String replaceName = objname.toString().replace(" ", "").replace("%","").replace(",","");
      return "https://www.cruzverde.cl/" + replaceName +"/"+ objId.toString() + ".html";
   }

}

