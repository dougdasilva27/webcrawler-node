package br.com.lett.crawlernode.crawlers.ranking.keywords.models;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.JSONUtils;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Date;

public class GPAKeywordsCrawler extends CrawlerRankingKeywords {
   private String keyword = this.keywordEncoded;
   protected String storeId = getStoreId();
   protected String store;
   protected String storeShort;
   protected String cep;
   protected String homePageHttps;
   private final String storeName = getStoreName();

   private static final String END_POINT_REQUEST = "https://api.gpa.digital/";

   public GPAKeywordsCrawler(Session session) {
      super(session);
      inferFields();
   }

   public String getStoreName() {
      return null;

   }

   public String getStoreId() {

      return session.getOptions().optString("storeId");
   }


   private void fetchStoreId() {
      Request request = RequestBuilder.create()
         .setUrl(END_POINT_REQUEST + this.storeShort + "/delivery/options?zipCode=" + this.cep.replace("-", ""))
         .setCookies(cookies)
         .build();

      String response = this.dataFetcher.get(session, request).getBody();
      JSONObject jsonObjectGPA = JSONUtils.stringToJson(response);
      if (jsonObjectGPA != null) {
         JSONArray jsonArrayDeliveryTypes = JSONUtils.getValueRecursive(jsonObjectGPA, "content.deliveryTypes", JSONArray.class);
         for (Object object : jsonArrayDeliveryTypes) {
            JSONObject deliveryType = (JSONObject) object;
            if (storeName != null && deliveryType.optString("storeName") != null && deliveryType.optString("storeName").contains(storeName)) {
               this.storeId = deliveryType.optString("storeid");
               break;
            } else if (storeName == null && deliveryType.optString("name") != null && deliveryType.optString("name").contains("TRADICIONAL")) {
               this.storeId = deliveryType.optString("storeid");
               break;
            }
         }

         if (storeId == null || storeId.isEmpty()){
            this.storeId = JSONUtils.getValueRecursive(jsonArrayDeliveryTypes, "0.storeid", Integer.class).toString();
         }
      }
   }

   private void inferFields() {
      String className = this.getClass().getSimpleName().toLowerCase();
      if (className.contains("paodeacucar")) {
         this.store = "paodeacucar";
         this.storeShort = "pa";
         this.homePageHttps = "https://www.paodeacucar.com/";
      } else if (className.contains("extra")) {
         this.store = "deliveryextra";
         this.storeShort = "ex";
         this.homePageHttps = "https://www.clubeextra.com.br/";
      }
   }

   @Override
   protected void processBeforeFetch() {
      if (this.cep != null) {
         fetchStoreId();
         BasicClientCookie cookie = new BasicClientCookie("ep.selected_store", this.storeId);
         cookie.setDomain(homePageHttps.substring(homePageHttps.indexOf("www"), homePageHttps.length() - 1));
         cookie.setPath("/");
         cookie.setExpiryDate(new Date(System.currentTimeMillis() + 604800000L + 604800000L));

         this.cookies.add(cookie);
      }
   }

   @Override
   public void extractProductsFromCurrentPage() {
      // número de produtos por página do market
      this.pageSize = 0;

      this.log("Página " + this.currentPage);
      JSONObject search = crawlSearchApi();

      if (search.has("results") && search.getJSONArray("results").length() > 0) {
         JSONArray products = search.getJSONArray("results");

         if (this.totalProducts == 0) {
            setTotalProducts(search);
         }

         for (int i = 0; i < products.length(); i++) {
            JSONObject product = products.getJSONObject(i);

            // Url do produto
            String productUrl = crawlProductUrl(product);

            // InternalPid
            String internalPid = crawlInternalPid(product);

            // InternalId
            String internalId = crawlInternalId(productUrl);

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

   @Override
   protected boolean hasNextPage() {
      return this.arrayProducts.size() < this.totalProducts;
   }

   protected void setTotalProducts(JSONObject search) {
      if (search.has("result_meta")) {
         JSONObject resultMeta = search.getJSONObject("result_meta");

         if (resultMeta.has("total")) {
            this.totalProducts = resultMeta.getInt("total");
            this.log("Total da busca: " + this.totalProducts);
         }
      }
   }

   private String crawlInternalId(String url) {
      String internalId = null;

      if (url != null) {
         internalId = CommonMethods.getLast(url.split("/"));
      }

      return internalId;
   }

   private String crawlInternalPid(JSONObject product) {
      String internalPid = null;

      if (product.has("sku")) {
         internalPid = product.getString("sku");
      }

      return internalPid;
   }

   private String crawlProductUrl(JSONObject product) {
      String urlProduct = null;

      if (product.has("url")) {
         urlProduct = product.getString("url");
      }

      return urlProduct;
   }

   private JSONObject crawlSearchApi() {
      StringBuilder aux = new StringBuilder();
      aux.append("https://").append(this.store).append(".resultspage.com/search?af=&cnt=36")
         .append("&isort=&lot=json&p=Q&")
         .append("ref=www.").append(this.store).append(".com.br&srt=").append(this.arrayProducts.size())
         .append("&ts=json-full")
         .append("&ua=Mozilla%2F5.0+(X11;+Linux+x86_64)+AppleWebKit%2F537.36+(KHTML,+like+Gecko)+Chrome%2F62.0.3202.62+Safari%2F537.36")
         .append("&w=").append(this.keyword);

      if (this.storeId != null) {
         aux.append("&ep.selected_store=").append(this.storeId);
      }

      String url = aux.toString();

      JSONObject searchApi = fetchJSONObject(url, cookies);

      if (this.currentPage == 1 && searchApi.has("merch")) {
         JSONObject merch = searchApi.getJSONObject("merch");
         if (merch.has("jumpurl")) {
            String jumpurl = merch.get("jumpurl").toString();
            if (jumpurl.contains("especial")) {
               String newKeyword = CommonMethods.getLast(jumpurl.split("\\?")[0].split("/"));
               url = url.replace(this.keyword, newKeyword);
               this.keyword = newKeyword;
               searchApi = fetchJSONObject(url, cookies);
            }
         }
      }

      this.log("Link onde são feitos os crawlers: " + url);

      return searchApi;
   }
}
