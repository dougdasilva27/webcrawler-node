package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import models.Offers;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class BrasilNestleAteVoceCrawler extends CrawlerRankingKeywords {
   public BrasilNestleAteVoceCrawler(Session session) {
      super(session);
   }

   private final String PASSWORD = getPassword();
   private final String LOGIN = getLogin();

   protected String getLogin() {
      return session.getOptions().optString("login");
   }

   protected String getPassword() {
      return session.getOptions().optString("password");
   }

   protected Map<String, String> fetchToken() {
      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/json");
      headers.put("x-authorization", "");

      String payload = "{\"operationName\":\"signIn\",\"variables\":{\"taxvat\":\"" + LOGIN + "\",\"password\":\"" + PASSWORD + "\"},\"query\":\"mutation signIn($taxvat: String!, $password: String!) {\\ngenerateCustomerToken(taxvat: $taxvat, password: $password) {\\ntoken\\nis_clube_nestle\\nenabled_club_nestle\\n__typename\\n}\\n}\\n\"}";

      Request requestToken = Request.RequestBuilder.create()
         .setUrl("https://www.nestleatevoce.com.br/graphql")
         .setHeaders(headers)
         .setPayload(payload)
         .setProxyservice(Arrays.asList(ProxyCollection.LUMINATI_SERVER_BR_HAPROXY, ProxyCollection.BUY, ProxyCollection.NETNUT_RESIDENTIAL_BR))
         .build();

      Response responseToken = CrawlerUtils.retryRequest(requestToken, session, new JsoupDataFetcher(), false);
      if (responseToken != null) {
         JSONObject objResponseToken = JSONUtils.stringToJson(responseToken.getBody());
         String token = JSONUtils.getValueRecursive(objResponseToken, "data.generateCustomerToken.token", String.class);
         headers.put("x-authorization", "Bearer " + token);
      }

      return headers;
   }

   private String getSessionUrl() {
      String variables = "{\"currentPage\":" + this.currentPage + ",\"pageSize\":20,\"filters\":{},\"inputText\":\"" + this.location.replace(" ", "+") + "\",\"sort\":{}}";
      try {
         String variablesEncoded = URLEncoder.encode(variables, "UTF-8");
         return "https://www.nestleatevoce.com.br/graphql?query=query+productSearch%28%24currentPage%3AInt%3D1%24inputText%3AString%21%24pageSize%3AInt%3D20%24filters%3AProductAttributeFilterInput%21%24sort%3AProductAttributeSortInput%29%7Bproducts%28currentPage%3A%24currentPage+pageSize%3A%24pageSize+search%3A%24inputText+filter%3A%24filters+sort%3A%24sort%29%7Bitems%7Burl_key+url_suffix+small_image%7Burl+__typename%7D...ProductCardFragment+__typename%7Dpage_info%7Btotal_pages+__typename%7Dtotal_count+__typename%7D%7Dfragment+ProductCardFragment+on+ProductInterface%7Bstock_status+__typename+id+sku+name+brand+combo_nrab_data%7Bis_nrab+qty_take+qty_buy+__typename%7Dscales%7Bqty+discount_percentage+__typename%7Dcategories%7Bid+name+url_key+__typename%7Dprice_range%7Bminimum_price%7Bfinal_price%7Bvalue+currency+__typename%7D__typename%7D__typename%7Dz076_data%7Bis_z076+discount_percent+__typename%7D...on+ConfigurableProduct%7Bis_assortment+is_best_seller+is_scaled+variants%7Battributes%7Bcode+label+value_index+__typename%7Dproduct%7Bid+sku+increments+units+stock_status+price_range%7Bminimum_price%7Bdiscount%7Bpercent_off+amount_off+__typename%7Dregular_price%7Bvalue+currency+__typename%7Dfinal_price%7Bvalue+currency+__typename%7D__typename%7D__typename%7D__typename%7D__typename%7Dconfigurable_options%7Bproduct_id+attribute_code+attribute_id+id+label+values%7Blabel+value_index+__typename%7D__typename%7D__typename%7D%7D&operationName=productSearch&variables=" + variablesEncoded;
      } catch (UnsupportedEncodingException e) {
         throw new RuntimeException(e);
      }
   }

   private JSONObject fetchJSONObject() {
      Map<String, String> headers = fetchToken();

      String requestURL = getSessionUrl();
      Request request = Request.RequestBuilder.create()
         .setUrl(requestURL)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(ProxyCollection.LUMINATI_RESIDENTIAL_BR, ProxyCollection.BUY, ProxyCollection.SMART_PROXY_BR_HAPROXY))
         .build();

      Response response = CrawlerUtils.retryRequest(request, session, new JsoupDataFetcher(), true);

      return CrawlerUtils.stringToJson(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      JSONObject bodyJson = fetchJSONObject();

      if (!bodyJson.isEmpty()) {
         if (this.currentPage == 1) {
            this.totalProducts = JSONUtils.getValueRecursive(bodyJson, "data.products.total_count", Integer.class);
         }
         JSONArray products = JSONUtils.getValueRecursive(bodyJson, "data.products.items", JSONArray.class);
         int alternativePosition = 1;

         if (!products.isEmpty()){
            for (Object o : products) {
               JSONObject product = (JSONObject) o;
               String internalPid = product.optString("sku");
               String productUrl = crawlProductUrl(product);
               String name = product.optString("name");
               String imageUrl = JSONUtils.getValueRecursive(product, "small_image.url", String.class);

               JSONArray variants = product.optJSONArray("variants");
               for (int i = 0; i < variants.length(); i++) {
                  JSONObject variantProduct = JSONUtils.getValueRecursive(variants, i + ".product", JSONObject.class);
                  String internalId = variantProduct.optString("id");
                  String variantName = name + " - " + JSONUtils.getValueRecursive(variants, i + ".attributes.0.label", String.class);

                  boolean availability = JSONUtils.getValueRecursive(variantProduct, "stock_status", String.class).equals("IN_STOCK");
                  Integer price = getPrice(availability, variantProduct);

                  RankingProduct productRanking = RankingProductBuilder.create()
                     .setUrl(productUrl)
                     .setInternalId(internalId)
                     .setInternalPid(internalPid)
                     .setName(variantName)
                     .setPriceInCents(price)
                     .setAvailability(availability)
                     .setImageUrl(imageUrl)
                     .setPosition(alternativePosition)
                     .build();

                  saveDataProduct(productRanking);
               }

               alternativePosition++;

               if (this.arrayProducts.size() == productsLimit)
                  break;
            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private Integer getPrice(boolean availability, JSONObject variantProduct) {
      Double priceDouble = JSONUtils.getValueRecursive(variantProduct, "price_range.minimum_price.final_price.value", Double.class, null);
      if (availability && priceDouble != null) {
         return (int) (priceDouble * 100);
      } else {
         return null;
      }
   }

   private String crawlProductUrl(JSONObject product) {
      String urlKey = product.optString("url_key");
      String urlSuffix = product.optString("url_suffix");
      return "https://www.nestleatevoce.com.br/" + urlKey + urlSuffix;
   }

   private String getInternalId(JSONObject product) {
      Double id = product.optDouble("id");
      return id != null ? id.toString() : null;
   }

   private String scrapName(JSONObject product) {
      String name = product.optString("productName");
      String brand = product.optString("brand");
      if (brand != null && !brand.isEmpty()) {
         return name + " - " + brand;
      }
      return name;
   }

   private String scrapImage(JSONObject item) {
      Object imageUrl = item.optQuery("/images/0/imageUrl");

      if (imageUrl instanceof String) {
         return (String) imageUrl;
      }
      return null;
   }

   private Integer scrapPrice(JSONObject item) {
      Object price = item.optQuery("/sellers/0/commertialOffer/Price");

      if (price instanceof Integer) {
         return (Integer) price;
      }

      return null;
   }
}
