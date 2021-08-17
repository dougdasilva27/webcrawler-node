package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.crawler.DiscoveryCrawlerSession;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Date: 05/11/20
 *
 * @author Fellype Layunne
 */
public abstract class RappiCrawlerRanking extends CrawlerRankingKeywords {

   private final String PRODUCTS_API_URL = "https://services." + getApiDomain() + "/api/cpgs/search/v2/store/";
   protected String PRODUCT_BASE_URL = "https://www." + getProductDomain() + "/product/";
   private final String STORE_ID = storeId();

   protected boolean newUnification = session.getOptions().optBoolean("newUnification",false);

   public RappiCrawlerRanking(Session session) {
      super(session);
   }

   protected String getStoreId(){
      return session.getOptions().optString("storeId");
   }

   @Deprecated
   protected  String getStoreType(){
      return "";
   }

   protected abstract String getApiDomain();

   protected abstract String getProductDomain();

   protected String storeId() {
      if(session.getOptions().optBoolean("newUnification", false)){
         return session.getOptions().optString("storeId");
      }
      else {
         return getStoreId();
      }
   }



   @Override
   public void extractProductsFromCurrentPage() {

      this.pageSize = 40;
      this.log("Página " + this.currentPage);

      JSONObject search;

      search = fetchProductsFromAPI(STORE_ID);

      // se obter 1 ou mais links de produtos e essa página tiver resultado
      if (search.has("products") && search.getJSONArray("products").length() > 0) {
         JSONArray products = search.getJSONArray("products");

//         // se o total de busca não foi setado ainda, chama a função para
//         if (this.totalProducts == 0) {
//            setTotalProducts(search);
//         }

         for (int i = 0; i < products.length(); i++) {
            JSONObject product = products.getJSONObject(i);

            String internalPid = crawlInternalPid(product);
            String internalId = newUnification ? internalPid : crawlInternalId(product);
            String productUrl = crawlProductUrl(product);

            saveDataProduct(internalId, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);

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

   protected void setTotalProducts(JSONObject search) {
      if (search.has("total_results") && search.get("total_results") instanceof Integer) {
         this.totalProducts = CrawlerUtils.getIntegerValueFromJSON(search, "total_results", 0);
         this.log("Total da busca: " + this.totalProducts);
      }
   }

   private String crawlInternalId(JSONObject product) {
      String internalId = null;

      if (product.has("id") && !product.isNull("id")) {
         internalId = product.get("id").toString();
      }

      return internalId;
   }

   private String crawlInternalPid(JSONObject product) {
      String internalPid = null;

      if (product.has("product_id") && !product.isNull("product_id")) {
         internalPid = product.get("product_id").toString();
      }

      return internalPid;
   }

   private String crawlProductUrl(JSONObject product) {
      String productUrl = null;

      if (product.has("id")) {
         String id = product.optString("id");
         productUrl = PRODUCT_BASE_URL + id;
      }

      return productUrl;
   }

   public JSONObject fetchProductsFromAPI(String storeId) {
      int startPage;

      if (currentPage == 1) {
         startPage = 0;
      } else {
         startPage = pageSize * (currentPage - 1);
      }


      String payload = "{\"from\":" + startPage + " ,\"query\":\"" + this.keywordWithoutAccents + "\",\"size\":" + pageSize + "}";

      String url = PRODUCTS_API_URL + storeId + "/products";

      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");
      headers.put("Accept", "application/json, text/plain, */*");

      Request request = RequestBuilder.create().setUrl(url).setHeaders(headers).setPayload(payload).mustSendContentEncoding(false).build();
      return CrawlerUtils.stringToJson(this.dataFetcher.post(session, request).getBody());
   }

   @Override
   protected boolean hasNextPage() {
      if (session instanceof DiscoveryCrawlerSession) {
         return true;
      } else {
         return false;
      }

   }
}
