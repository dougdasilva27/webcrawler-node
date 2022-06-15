package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.crawler.DiscoveryCrawlerSession;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

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

   private Document fetch(String url) {
      BasicClientCookie cookie = new BasicClientCookie("currentLocation", "eyJhZGRyZXNzIjoiMDYwOTAtMDEwLCBBdmVuaWRhIGRvcyBBdXRvbm9taXN0YXMgLSBDZW50cm8sIE9zYXNjbyAtIFN0YXRlIG9mIFPjbyBQYXVsbywgQnJhemlsIiwic2Vjb25kYXJ5TGFiZWwiOiJBdmVuaWRhIGRvcyBBdXRvbm9taXN0YXMgLSBDZW50cm8sIE9zYXNjbyAtIFN0YXRlIG9mIFPjbyBQYXVsbywgQnJhemlsIiwiZGlzdGFuY2VJbkttcyI6MTEuNiwicGxhY2VJZCI6IkNoSUpsemdyMlJMX3pwUVJoWTBsMmdrSTFlayIsInBsYWNlSW5mb3JtYXRpb24iOm51bGwsInNvdXJjZSI6Imdvb2dsZSIsImlkIjoxLCJkZXNjcmlwdGlvbiI6IiIsImxhdCI6LTIzLjUzNzYwNTYsImxuZyI6LTQ2Ljc3Njc5NzU5OTk5OTk5LCJjb3VudHJ5IjoiQnJhemlsIiwiYWN0aXZlIjp0cnVlfQ==");
      cookie.setDomain(".www." + getProductDomain());
      cookie.setPath("/");
      cookies.add(cookie);

      Request request = RequestBuilder.create()
         .setCookies(cookies)
         .setUrl(url)
         .setFollowRedirects(true)
         .build();

      Response response = dataFetcher.get(session, request);

      return Jsoup.parse(response.getBody());
   }

   @Override
   public void extractProductsFromCurrentPage() throws MalformedProductException {

      this.pageSize = 40;
      this.log("Página " + this.currentPage);

      String marketUrl = "https://www.rappi.com.br/lojas/" + getStoreId();
      this.currentDoc = fetch(marketUrl + "/s?term=" + this.keywordEncoded);


      JSONObject pageJson = CrawlerUtils.selectJsonFromHtml(this.currentDoc, "#__NEXT_DATA__", null, null, false, false);
      JSONArray products = JSONUtils.getValueRecursive(pageJson, "props.pageProps.products", JSONArray.class, new JSONArray());

      if (products.length() > 0) {
         for (int i = 0; i < products.length(); i++) {
            JSONObject product = products.getJSONObject(i);

            String internalId = product.optString("product_id");
            String id = product.optString("id", "");
            String url = !id.equals("") ? PRODUCT_BASE_URL + id : "";
            String name = product.optString("name");
            Integer priceInCents = scrapPrice(product);
            boolean isAvailable = product.optBoolean("in_stock");
            String imageUrl = crawlProductImage(product);

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(url)
               .setInternalId(internalId)
               .setName(name)
               .setPriceInCents(priceInCents)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
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

   private String crawlProductImage(JSONObject product) {
      return CrawlerUtils.completeUrl(product.optString("image"), "https", "images.rappi.com.br/products");
   }


   private Integer scrapPrice(JSONObject product) {
      double price = product.optDouble("price");
      Integer priceInCents = null;
      if (price != 0.0) {
         priceInCents = Integer.parseInt(Double.toString(price).replace(".", ""));
      }
      return priceInCents;
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

   protected JSONObject fetchProductsFromAPI(String storeId) {
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
