package br.com.lett.crawlernode.crawlers.ranking.keywords.guatemala;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class GuatemalaSelecciondelchefCrawler extends CrawlerRankingKeywords {

   public GuatemalaSelecciondelchefCrawler(Session session) {
      super(session);
   }

   JSONObject fetchProductInfo (){

      String buildUrl = "https://app.ecwid.com/api/v3/7045371/products?" +
         "token=public_iySXFRtQ5p1hKmDLHxTGPFvTd8SW9asG" +
         "&keyword=" + this.keywordWithoutAccents;

      Request resquest = Request.RequestBuilder.create().setUrl(buildUrl).build();
      String response = this.dataFetcher.get(session,resquest).getBody();

      return JSONUtils.stringToJson(response);

   }

   JSONArray selectObjects (JSONArray items){
      JSONArray listOfProducts = new JSONArray();

      for (int i = 0; i <= items.length(); i++){
         JSONObject o = (JSONObject) items.opt(i);
         if(o != null) {
            if (!o.optString("sku").isEmpty()) {
               listOfProducts.put(o);
            }
         }
      }
      return listOfProducts;
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException {
      this.log("Página " + this.currentPage);

      JSONObject productsInfo = fetchProductInfo();
      JSONArray items = productsInfo.optJSONArray("items");

      JSONArray selectedItems = selectObjects(items);


      if (items.length() >= 1) {
         for (Object e : selectedItems) {

            JSONObject product = (JSONObject) e;

            String internalId = product.optString("sku");
            String productUrl = product.optString("url");

            saveDataProduct(internalId, internalId, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalId + " - Url: " + productUrl);

         }
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }
}
