package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import com.google.common.net.HttpHeaders;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
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
      HttpResponse<String> response;
      Map<String, String> headers = new HashMap<>();

      String payload = "{\"operationName\":\"signIn\",\"variables\":{\"taxvat\":\"" + LOGIN + "\",\"password\":\"" + PASSWORD + "\", \"chatbot\":null},\"query\":\"mutation signIn($taxvat: String!, $password: String!, $chatbot: String) {\\ngenerateCustomerToken(taxvat: $taxvat, password: $password, chatbot: $chatbot) {\\ntoken\\nis_clube_nestle\\nenabled_club_nestle\\n__typename\\n}\\n}\\n\"}";

      try {
         HttpClient client = HttpClient.newBuilder().build();
         HttpRequest request = HttpRequest.newBuilder()
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .header(HttpHeaders.ACCEPT, "*/*")
            .header(HttpHeaders.ORIGIN, "https://www.nestleatevoce.com.br")
            .header(HttpHeaders.REFERER, "https://www.nestleatevoce.com.br/login")
            .header("x-authorization", "")
            .uri(URI.create("https://www.nestleatevoce.com.br/graphql"))
            .build();

         response = client.send(request, HttpResponse.BodyHandlers.ofString());
      } catch (Exception e) {
         throw new RuntimeException("Failed in load document: " + session.getOriginalURL(), e);
      }
      if (response != null) {
         JSONObject objResponseToken = JSONUtils.stringToJson(response.body());
         String token = JSONUtils.getValueRecursive(objResponseToken, "data.generateCustomerToken.token", String.class);
         headers.put("x-authorization", "Bearer " + token);
      }

      return headers;
   }

   private String getSessionUrl() {
      String variables = "{\"currentPage\":" + this.currentPage + ",\"pageSize\":20,\"filters\":{\"name\":{\"match\":\"" + this.keywordEncoded + "\"}},\"inputText\":\"" + this.keywordEncoded + "\",\"sort\":{\"relevance\":\"ASC\"}}";
      String variablesEncoded = URLEncoder.encode(variables, StandardCharsets.UTF_8);
      return "https://www.nestleatevoce.com.br/graphql?query=query+productSearch%28%24currentPage%3AInt%3D1%24inputText%3AString%21%24pageSize%3AInt%3D20%24filters%3AProductAttributeFilterInput%21%24sort%3AProductAttributeSortInput%29%7Bproducts%28currentPage%3A%24currentPage+pageSize%3A%24pageSize+search%3A%24inputText+filter%3A%24filters+sort%3A%24sort%29%7Bitems%7Burl_key+url_suffix+small_image%7Burl+__typename%7D...ProductCardFragment+__typename%7Dpage_info%7Btotal_pages+__typename%7Dtotal_count+__typename%7D%7Dfragment+ProductCardFragment+on+ProductInterface%7Bstock_status+__typename+is_regular_combo+is_assortment+is_best_seller+is_scaled+id+sku+name+brand+media_gallery_entries%7Blabel+file+__typename%7Dcombo_nrab_data%7Bis_nrab+qty_take+qty_buy+__typename%7Dscales%7Bqty+discount_percentage+__typename%7Dcategories%7Bid+name+url_key+__typename%7Dprice_range%7Bminimum_price%7Bfinal_price%7Bvalue+currency+__typename%7D__typename%7D__typename%7Dz076_data%7Bis_z076+discount_percent+__typename%7D...on+ConfigurableProduct%7Bvariants%7Battributes%7Bcode+label+value_index+__typename%7Dproduct%7Bid+sku+increments+units+stock_status+price_range%7Bminimum_price%7Bdiscount%7Bpercent_off+amount_off+__typename%7Dregular_price%7Bvalue+currency+__typename%7Dfinal_price%7Bvalue+currency+__typename%7D__typename%7D__typename%7D...on+SimpleProduct%7Bincrements+units+sku+__typename+best_seller_percentage+ean+price_range%7Bminimum_price%7Bregular_price%7Bvalue+currency+__typename%7Dfinal_price%7Bvalue+currency+__typename%7Ddiscount%7Bamount_off+percent_off+__typename%7D__typename%7D__typename%7D%7D__typename%7D__typename%7Dconfigurable_options%7Bproduct_id+attribute_code+attribute_id+id+label+values%7Blabel+value_index+__typename%7D__typename%7D__typename%7D...on+BundleProduct%7Bprice_range%7Bminimum_price%7Bregular_price%7Bvalue+currency+__typename%7Dfinal_price%7Bvalue+currency+__typename%7Ddiscount%7Bamount_off+percent_off+__typename%7D__typename%7D__typename%7Ditems%7Btitle+sku+options%7Buid+quantity+position+price+price_type+can_change_quantity+label+product%7Buid+name+sku+__typename%7D__typename%7D__typename%7D__typename%7D%7D&operationName=productSearch&variables=" + variablesEncoded;
   }

   private JSONObject fetchJSONObject() {
      Map<String, String> headers = fetchToken();
      String header = "x-authorization";
      String requestURL = getSessionUrl();
      HttpResponse<String> response;

      try {
         HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
         HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .header(header, headers.get(header))
            .uri(URI.create(requestURL))
            .build();
         response = client.send(request, HttpResponse.BodyHandlers.ofString());
      } catch (Exception e) {
         throw new RuntimeException("Failed in load document: " + requestURL, e);
      }

      return CrawlerUtils.stringToJson(response.body());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      JSONObject bodyJson = fetchJSONObject();

      if (bodyJson != null && !bodyJson.isEmpty()) {
         if (this.currentPage == 1) {
            this.totalProducts = JSONUtils.getValueRecursive(bodyJson, "data.products.total_count", Integer.class, 0);
         }
         JSONArray products = JSONUtils.getValueRecursive(bodyJson, "data.products.items", JSONArray.class, new JSONArray());

         int alternativePosition = ((this.currentPage - 1) * 20) + 1;
         if (!products.isEmpty()) {
            for (Object o : products) {
               JSONObject product = (JSONObject) o;
               String internalPid = product.optString("sku");
               String productUrl = crawlProductUrl(product);
               String name = product.optString("name");
               String imageUrl = JSONUtils.getValueRecursive(product, "small_image.url", String.class);

               JSONArray variants = product.optJSONArray("variants");
               if (variants == null) {
                  String internalId = product.optString("id");

                  boolean availability = product.optString("stock_status").equals("IN_STOCK");
                  Integer price = getPrice(availability, product);
                  RankingProduct productRanking = RankingProductBuilder.create()
                     .setUrl(productUrl)
                     .setInternalId(internalId)
                     .setInternalPid(internalPid)
                     .setName(name)
                     .setPriceInCents(price)
                     .setAvailability(availability)
                     .setImageUrl(imageUrl)
                     .setPosition(alternativePosition)
                     .build();

                  saveDataProduct(productRanking);
               } else {
                  for (int i = 0; i < variants.length(); i++) {
                     JSONObject variantProduct = JSONUtils.getValueRecursive(variants, i + ".product", JSONObject.class);
                     JSONArray variantAttributes = JSONUtils.getValueRecursive(variants, i + ".attributes", JSONArray.class);
                     String internalId = variantProduct.optString("id");
                     String variantName = crawlVariantName(variantAttributes, name);

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

   private String crawlVariantName(JSONArray variantAttributes, String name) {
      String label = JSONUtils.getValueRecursive(variantAttributes, "0.label", String.class);
      if (label != null) {
         if (label.equals("CS")) {
            return name + " - CAIXA";
         }
         if (label.equals("DS")) {
            return name + " - DISPLAY";
         }
         if (label.equals("EA")) {
            return name + " - UNIDADE";
         }
         return name + " - " + label;
      }

      return name;
   }

   private Integer getPrice(boolean availability, JSONObject variantProduct) {
      Double priceDouble = JSONUtils.getValueRecursive(variantProduct, "price_range.minimum_price.final_price.value", Double.class, null);
      if (availability && priceDouble != null) {
         return CommonMethods.doublePriceToIntegerPrice(new BigDecimal(priceDouble).setScale(2, RoundingMode.HALF_EVEN).doubleValue(), null);
      } else {
         return null;
      }
   }

   private String crawlProductUrl(JSONObject product) {
      String urlKey = product.optString("url_key");
      String urlSuffix = product.optString("url_suffix");
      return "https://www.nestleatevoce.com.br/" + urlKey + urlSuffix;
   }
}
