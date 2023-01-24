package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilCarrefourFetchUtils;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.eclipse.jetty.util.ajax.JSON;
import org.json.JSONArray;
import org.json.JSONObject;
import org.yaml.snakeyaml.util.UriEncoder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class CarrefourMercadoRanking extends CrawlerRankingKeywords {

   public CarrefourMercadoRanking(Session session) {
      super(session);
      dataFetcher = new JsoupDataFetcher();
      this.pageSize = 19;
   }

   private static final String OPERATION_NAME = "SearchQuery";
   private static final String SHA256 = "e0cccb090ae312126d6b49303fa1fb8105e6cf883d8d307c5b73b1436c427cce";
   public static final String HOME_PAGE = "https://mercado.carrefour.com.br/";

   @Override
   protected void processBeforeFetch() {
      super.processBeforeFetch();

      if (getCep() != null) {
         BasicClientCookie userLocationData = new BasicClientCookie("userLocationData", getCep());
         userLocationData.setPath("/");
         cookies.add(userLocationData);
      }

      if (getLocation() != null) {
         BasicClientCookie vtexSegment = new BasicClientCookie("vtex_segment", getLocation());
         vtexSegment.setPath("/");
         this.cookies.add(vtexSegment);
      }
   }

   protected  String getHomePage(){
      return HOME_PAGE;
   }

   protected String getLocation(){
      return session.getOptions().optString("vtex_segment");
   }

   protected String getCep() {
      return this.session.getOptions().optString("cep");
   }

   protected String getRegionId() {
      return "v2.0E68B2A9BF71F2194EF2890250AAAAF4";
   }

   protected String buildUrl(String homepage) {
      StringBuilder url = new StringBuilder();
      url.append(homepage);
      url.append("graphql/");
      url.append("?operationName=" + OPERATION_NAME);

      JSONObject extensions = new JSONObject();
      JSONObject persistedQuery = new JSONObject();

      persistedQuery.put("sha256Hash", SHA256);
      extensions.put("persistedQuery", persistedQuery);

      url.append("&extensions=").append(URLEncoder.encode(extensions.toString(), StandardCharsets.UTF_8));

      JSONObject variables = new JSONObject();
      variables.put("fullText", URLEncoder.encode(this.location, StandardCharsets.UTF_8).replace("+", "%20"));
      variables.put("sort", "");
      variables.put("from", this.arrayProducts.size());
      variables.put("to", this.arrayProducts.size() + this.pageSize);

      JSONArray selectedFacets = new JSONArray();

      JSONObject facet = new JSONObject();
      facet.put("key", "region-id");
      facet.put("value", BrasilCarrefourFetchUtils.getRegionId(dataFetcher, getCep(), session));

      selectedFacets.put(facet);

      variables.put("selectedFacets", selectedFacets);

      String variablesBase64 = "\"" + Base64.getEncoder().encodeToString(variables.toString().getBytes(StandardCharsets.UTF_8)) + "\"";

      url.append("&variables=").append(URLEncoder.encode(variablesBase64, StandardCharsets.UTF_8));

      return url.toString();
   }

   private JSONObject fetchResponse() {
      String hash = "c7192b2bcf338b74cf67aabc37f24634920025b52e8849393bcf1c547e598d9b";

      String regionId = getRegionId();
      String extensions = "{\"persistedQuery\":{\"sha256Hash\":\"" + hash + "\"}}";
      String variables = "{\"fullText\": \"" + this.keywordEncoded + "\",\"selectedFacets\": [{\"key\": \"region-id\",\"value\": \"" + regionId + "\"}],\"sort\": \"\",\"from\": " + (this.currentPage - 1) * this.pageSize + ",\"to\": " + (this.currentPage * this.pageSize - 1) + "}";
      String variablesBase64 = Base64.getEncoder().encodeToString(variables.getBytes());

      StringBuilder url = new StringBuilder();
      url.append("https://mercado.carrefour.com.br/graphql/?operationName=SearchQuery");
//      url.append(extensions);
      url.append("&extensions=").append(URLEncoder.encode(extensions.toString(), StandardCharsets.UTF_8));


      url.append("&variables=");
      url.append(variablesBase64);

      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "*/*");
      headers.put("authority", "mercado.carrefour.com.br");
      headers.put("referer", "https://mercado.carrefour.com.br/");

      Request request = Request.RequestBuilder.create()
         .setUrl("https://mercado.carrefour.com.br/graphql/?operationName=SearchQuery&extensions={\"persistedQuery\":{\"sha256Hash\":\"c7192b2bcf338b74cf67aabc37f24634920025b52e8849393bcf1c547e598d9b\"}}&variables=\"eyJmdWxsVGV4dCI6ImNlcnZlamEiLCJzZWxlY3RlZEZhY2V0cyI6W3sia2V5IjoicmVnaW9uLWlkIiwidmFsdWUiOiJ2Mi4wRTY4QjJBOUJGNzFGMjE5NEVGMjg5MDI1MEFBQUFGNCJ9XSwic29ydCI6IiIsImZyb20iOjAsInRvIjoxNX0=\"")
         .setHeaders(headers)
         .setCookies(this.cookies)
         .build();

      return CrawlerUtils.stringToJSONObject(new FetcherDataFetcher().get(session, request).getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage()  {
      this.pageSize = 16;
      this.log("Página " + this.currentPage);

      String homePage = getHomePage();
      String url = buildUrl(homePage);

      JSONObject jsonResponse = fetchResponse();
      JSONArray products = JSONUtils.getValueRecursive(jsonResponse, "data.vtex.productSearch.products",  JSONArray.class);

      if (products != null && !products.isEmpty()) {
         for (Object object : products) {

            JSONObject product = (JSONObject) object;
            String productUrl = CrawlerUtils.completeUrl(product.optString("linkText"), "https",
               this.getHomePage().replace("https://", "".replace("http://", ""))) + "/p";
            String internalPid = product.optString("id");
            String name = product.optString("productName");
            String image = scrapImage(product);
            Integer price = scrapPrice(product);

            try {
               RankingProduct rankingProduct = RankingProductBuilder.create()
                  .setInternalPid(internalPid)
                  .setUrl(productUrl)
                  .setName(name)
                  .setImageUrl(image)
                  .setPriceInCents(price)
                  .setAvailability(price != null)
                  .setPageNumber(this.currentPage)
                  .build();

               saveDataProduct(rankingProduct);
            } catch (MalformedProductException e) {
               this.log(e.getMessage());
            }

            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }
      }
   }

   private String scrapImage(JSONObject product) {
      String image = null;

      JSONArray items = product.optJSONArray("items");
      if(items != null && !items.isEmpty()) {
         JSONObject item = items.optJSONObject(0);

         JSONArray images = item.optJSONArray("images");
         if(images != null && !images.isEmpty()) {
            JSONObject imageJson = images.optJSONObject(0);

            image = imageJson.optString("imageUrl");
         }
      }

      return image;
   }

   private Integer scrapPrice(JSONObject product) {
      Integer price = null;

      JSONArray items = product.optJSONArray("items");
      if(items != null && !items.isEmpty()) {
         JSONObject item = items.optJSONObject(0);

         if(item != null) {
            JSONArray sellers = item.optJSONArray("sellers");
            if(sellers != null && !sellers.isEmpty()) {
               JSONObject seller = sellers.optJSONObject(0);

               Double priceDouble = JSONUtils.getValueRecursive(seller, "commercialOffer.price", Double.class);
               if(priceDouble != null) {
                  price = ((Double) (priceDouble * 100d)).intValue();
               }
            }
         }
      }

      return price;
   }

   @Override
   protected boolean hasNextPage() {
      return ((arrayProducts.size() - 1) % pageSize - currentPage) < 0;
   }
}
