package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.JSONUtils;
import com.mongodb.util.JSON;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilTintasmcCrawler extends CrawlerRankingKeywords {

   public BrasilTintasmcCrawler(Session session) {
      super(session);
   }

   private String buildURLToRequest() {
      String urlToRequest = "";

      String protocolApi = "https://www.obrazul.com.br/api/search/products-v2/?user_location=";

      JSONObject location = new JSONObject();
      JSONObject address = new JSONObject();

      location.put("lat", "-23.551418");
      location.put("lng", "-46.72117410000001");
      location.put("full_address", "Av. das Nações Unidas - Alto de Pinheiros, São Paulo - SP, 05466, Brasil");
      location.put("address", address);
      location
         .put("place_id", "EktBdi4gZGFzIE5hw6fDtWVzIFVuaWRhcyAtIEFsdG8gZGUgUGluaGVpcm9zLCBTw6NvIFBhdWxvIC0gU1AsIDA1NDY2LCBCcmF6aWwiLiosChQKEgm_RA79OlbOlBHcB5b-x2OnrhIUChIJs5ptPTFWzpQRdU3tPpsXy-I");

      address.put("address", "Avenida das Nações Unidas");
      address.put("neighborhood", "Alto de Pinheiros");
      address.put("city", "São Paulo");
      address.put("state", "SP");
      address.put("_state", "São Paulo");
      address.put("country", "BR");
      address.put("_country", "Brasil");
      address.put("postal_code", "05466");

      String locationString = CommonMethods.encondeStringURLToISO8859(location.toString(), logger, session);
      urlToRequest = protocolApi + locationString + "&search=" + this.keywordWithoutAccents;

      return urlToRequest;
   }


   private JSONArray fetch() {
      String url = buildURLToRequest();

      Map<String, String> headers = new HashMap<>();
      headers.put("authorization", "token 70f16006cb76009ad4d6448910360b5ff55eb9e1"); //This is not the best way to set the token but when this scraper was created this was the only way found...

      Request request = Request.RequestBuilder.create().setHeaders(headers).setUrl(url).build();
      JSONObject jsonObject = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
      JSONArray productsArr = JSONUtils.getJSONArrayValue(jsonObject, "products");


      return productsArr;
   }

   private String scrapInternalId(JSONObject prod) {
      String internalId = "";
      JSONArray stores = JSONUtils.getJSONArrayValue(prod, "stores");

      if (!stores.isEmpty() && stores != null) {
         for (Object arr : stores) {
            JSONObject store = (JSONObject) arr;
            internalId = store.optString("productstore_id");
         }
      }
      return internalId;
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 12;
      this.log("Página " + this.currentPage);


      JSONArray products = fetch();

      if (!products.isEmpty()) {
         for (Object object : products) {
            JSONObject prod = (JSONObject) object;

            String internalId = scrapInternalId(prod);
            String productUrl = "https://loja.tintasmc.com.br/produtos/" + prod.optString("slug");

            saveDataProduct(internalId, null, productUrl);

            this.log(
               "Position: " + this.position +
                  " - InternalId: " + internalId +
                  " - InternalPid: " + null +
                  " - Url: " + productUrl);

            if (this.arrayProducts.size() == productsLimit)
               break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }
      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");
   }

}
