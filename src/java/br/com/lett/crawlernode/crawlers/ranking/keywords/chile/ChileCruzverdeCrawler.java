package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class ChileCruzverdeCrawler extends CrawlerRankingKeywords {

   public ChileCruzverdeCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   private final int STORE_ID = session.getOptions().optInt("store_id", 1121);


   @Override
   protected void processBeforeFetch() {
      Request request = Request.RequestBuilder.create()
         .setUrl("https://api.cruzverde.cl/customer-service/login")
         .build();
      Response responseApi = new JsoupDataFetcher().post(session, request);
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
         .build();
      Response response = this.dataFetcher.get(session, request);
      if (response != null) {
         return CrawlerUtils.stringToJson(response.getBody());
      }
      return null;
   }

   private String getInternalId(JSONObject prodList) {
      return prodList.optString("productId");
   }

   private Integer getPrice(JSONObject obj, String id) {
      Object stockPrice = obj.optQuery("/" + id + "/prices/price-sale-cl");
      if (stockPrice == null){
         stockPrice = obj.optQuery("/" + id + "/prices/price-list-cl");
      }
      return Integer.parseInt(stockPrice.toString());
   }

   private boolean getAvaliability(JSONObject obj, String id) {

      Object stock = obj.optQuery("/" + id + "/stock");
      return stock != null;
   }

   private JSONObject getAvaliPrice(String id) {
      JSONObject obj = null;
      String url = "https://api.cruzverde.cl/product-service/products/product-summary?ids=" + id + "&ids=" + id + "&fields=stock&fields=prices&fields=promotions&inventoryId=" + STORE_ID;
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .mustSendContentEncoding(true)
         .setCookies(this.cookies)
         .build();
      Response response = this.dataFetcher.get(session, request);
      if (response != null) {
         obj = CrawlerUtils.stringToJson(response.getBody());
      }

      return obj;
   }

   private String getImg(JSONObject prodList) {
      String img = "";
      Object objImg = prodList.optQuery("/image/link");
      if (objImg != null){
         img = objImg.toString();
      }
      return img;

   }

   private String getName(JSONObject prodList) {
      String name = "";
      Object objName = prodList.optString("productName");
      if (objName != null){
         name = objName.toString();
      }
      return name;
   }

   private String getProductUrl(JSONObject prodList) {
      String objname = prodList.optString("productName");
      String objId = prodList.optString("productId");
      String replaceName = objname.replace(" ", "").replace("%","").replace(",","");
      return "https://www.cruzverde.cl/" + replaceName +"/"+ objId + ".html";
   }

}

