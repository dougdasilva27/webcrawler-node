package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.methods.JavanetDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class BrasilSemardriveCrawler extends CrawlerRankingKeywords {

   public BrasilSemardriveCrawler(Session session) {
      super(session);
   }

   protected String getProductsList() {
      String url = "https://www.semarentrega.com.br/ccstoreui/v1/search?suppressResults=false&searchType=simple&No=1&Nrpp=24&Ntt=" + keywordEncoded + "&page=" + (currentPage - 1);

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .build();

      String response = dataFetcher.get(session, request).getBody();

      return response;

   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      String response = getProductsList();
      JSONObject reponseJson = CrawlerUtils.stringToJson(response);
      this.totalProducts= reponseJson.getJSONObject("resultsList").getInt("totalNumRecs");
      JSONArray productsList = reponseJson.getJSONObject("resultsList").getJSONArray("records");

      for (Object arrayOfArrays : productsList) {
         JSONObject jsonInfo = (JSONObject) arrayOfArrays;
         JSONArray records = jsonInfo.getJSONArray("records");
         String productUrl = "https://www.semarentrega.com.br" + records.getJSONObject(0).getJSONObject("attributes").getJSONArray("product.route").getString(0);
         String internalId = jsonInfo.getJSONObject("attributes").getJSONArray("product.repositoryId").getString(0);
         String name = records.getJSONObject(0).getJSONObject("attributes").getJSONArray("sku.displayName").getString(0);
         String imgUrl = "https://www.semarentrega.com.br" + records.getJSONObject(0).getJSONObject("attributes").getJSONArray("product.primaryFullImageURL").getString(0);
         String Sprice = records.getJSONObject(0).getJSONObject("attributes").getJSONArray("product.listPrice").getString(0);
         Double Dprice = Double.parseDouble(Sprice)*100;
         Integer price = Dprice.intValue();
         boolean isAvailable = isAvailable(internalId);
         RankingProduct productRanking = RankingProductBuilder.create()
            .setUrl(productUrl)
            .setInternalId(internalId)
            .setName(name)
            .setImageUrl(imgUrl)
            .setPriceInCents(price)
            .setAvailability(isAvailable)
            .build();

         saveDataProduct(productRanking);

      }
   }
   protected Boolean isAvailable(String code){
      String url = "https://www.semarentrega.com.br/ccstore/v1/inventories?fields=skuId,locationInventoryInfo,stockLevel";
      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");

      JSONObject payload = new JSONObject();
      payload.put("ids", code);
      payload.put("locationIds", session.getOptions().optString("storeId"));

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload.toString())
         .build();
      String response = new JavanetDataFetcher().post(session, request).getBody();
      JSONObject reponseJson = CrawlerUtils.stringToJson(response);
      String stock = reponseJson.getJSONArray("items").getJSONObject(0).getJSONArray("locationInventoryInfo").getJSONObject(0).getString("availabilityStatusMsg");
      boolean Bstock = stock == "inStock" ? true : false;
      return  Bstock;
      
   }

}

