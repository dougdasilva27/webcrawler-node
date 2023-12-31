package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

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
import java.util.HashMap;
import java.util.Map;

public abstract class TintasmcRanking extends CrawlerRankingKeywords {

   public TintasmcRanking(Session session) {
      super(session);
   }

   /**
    * Can be found after insert address on the website. Request: 'https://www.obrazul.com.br/api/search/products-v2'
    * <p>
    * Format:
    * {"address":{"country":"BR","_country":"Brasil","address":"","city":"","_state":"","neighborhood":"","state":"","postal_code":""},"lng":"","full_address":"","lat":"","place_id":""}
    *
    * @return location data in json format
    */
   protected abstract String setJsonLocation();

   private String buildURLToRequest() {
      String protocolApi = "https://www.obrazul.com.br/api/search/products-v2/?user_location=";

      String locationJson = setJsonLocation();
      String locationString = CommonMethods.encondeStringURLToISO8859(locationJson, logger, session);

      return protocolApi + locationString + "&search=" + this.keywordEncoded;
   }

   private JSONArray fetch() {
      String url = buildURLToRequest();

      Map<String, String> headers = new HashMap<>();
      headers.put("authorization", "token 70f16006cb76009ad4d6448910360b5ff55eb9e1"); //This is not the best way to set the token but when this scraper was created this was the only way found...

      Request request = Request.RequestBuilder.create().setHeaders(headers).setUrl(url).build();
      JSONObject jsonObject = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
      return JSONUtils.getJSONArrayValue(jsonObject, "products");
   }

   private String scrapInternalId(JSONObject prod) {
      String internalId = "";
      JSONArray stores = JSONUtils.getJSONArrayValue(prod, "stores");

      if (!stores.isEmpty()) {
         for (Object arr : stores) {
            JSONObject store = (JSONObject) arr;
            internalId = store.optString("productstore_id");
         }
      }
      return internalId;
   }
   private String scrapName(JSONObject prod){
      try {
         return prod.optString("fullname");
      }catch (NullPointerException e){
         return "";
      }
   }
   private String scrapImg(JSONObject prod){
      try {
         return prod.optString("image");
      }catch (NullPointerException e){
         return "";
      }
   }
   private Integer scrapPrice(JSONObject prod){
      try {
         Double priceDouble = prod.optJSONArray("stores").optJSONObject(0).optDouble("price");
         Integer priceInCents = (int) Math.round(100 * priceDouble);
         return  priceInCents;
      }catch (NullPointerException e){
         return 0;
      }
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 12;

      JSONArray products = fetch();

      if (!products.isEmpty()) {
         for (Object object : products) {
            JSONObject prod = (JSONObject) object;

            String internalId = scrapInternalId(prod);
            String productUrl = "https://loja.tintasmc.com.br/produtos/" + prod.optString("slug");
            String name = scrapName(prod);
            String imgUrl = scrapImg(prod);
            Integer price = scrapPrice(prod);
            boolean isAvailable = price!=0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setName(name)
               .setImageUrl(imgUrl)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);
            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }
      }
      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");
   }

}
