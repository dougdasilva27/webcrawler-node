package br.com.lett.crawlernode.crawlers.ranking.keywords.costarica;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CostaricaAutomercadoCrawler extends CrawlerRankingKeywords {

   public CostaricaAutomercadoCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 26;

      JSONObject apiJson = fetchProducts();

      if (apiJson != null && !apiJson.isEmpty()) {
         JSONObject result = JSONUtils.getValueRecursive(apiJson, "data.products", JSONObject.class);
         if (this.totalProducts == 0) {
            this.totalProducts = result.optInt("count", 0);
         }
         JSONArray products = result.optJSONArray("content");
         for (Object o : products) {
            JSONObject product = (JSONObject) o;

            String internalId = product.optString("uuid");
            String internalPid = product.optString("productNumber");
            String name = product.optString("name");
            String url = getUrl(internalId, name);
            String imgUrl = product.optString("icon");
            Integer price = product.optInt("rawPrice", 0) * 100;
            boolean isAvailable = product.optBoolean("productAvailable");

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(url)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setImageUrl(imgUrl)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);
         }

      } else {
         log("keyword sem resultado");
      }

   }

   private String getUrl(String internalId, String name) {
      String url = null;
      String nameEncoded;
      if (name != null && !name.isEmpty()){
          nameEncoded = CommonMethods.toSlug(name);
      } else {
         nameEncoded = "%20";
      }
      if (internalId != null) {
         url = "https://www.automercado.cr/p/" + nameEncoded + "/id/" + internalId;
      }
      return url;
   }


   private JSONObject fetchProducts() {
      String payload = "{\"search\":\""+this.keywordWithoutAccents+"\",\"page\":0,\"facetFilters\":{\"brand\":[],\"lifeStyle\":[],\"department\":[],\"category\":null,\"subCategory\":[]}}";

      String authHeader = session.getOptions().optString("authHeader");
      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/json");
      headers.put("Authorization", authHeader);

      Request request = Request.RequestBuilder.create()
         .setUrl("https://automercado.azure-api.net/prod-front/product/searchByAttributes")
         .setPayload(payload)
         .setHeaders(headers)
         .mustSendContentEncoding(false)
         .build();

      String content = "{}";
      int tries = 0;

      while (content.equals("{}") && tries < 3) {
         content = this.dataFetcher.post(session, request).getBody();
         tries++;
      }

      return CrawlerUtils.stringToJson(content);
   }
}
