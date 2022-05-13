package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.LinxImpulseRanking;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BrasilCompreAgoraCrawler extends LinxImpulseRanking {
   public BrasilCompreAgoraCrawler(Session session) {
      super(session);
   }

   private Integer price;
   private String internalPid;
   private String internalId;
   private JSONArray pricesJson;

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      String url = mountURL();
      JSONObject data = fetchPage(url);
      JSONArray products = data.optJSONArray("products");
      Integer position = 1;

      if (products != null && !products.isEmpty()) {
         String productIds = mountUrlid(products);
         pricesJson = fetchPrice(productIds);
         this.totalProducts = data.optInt("size");
         for (Object object : products) {
            JSONObject product = (JSONObject) object;
            internalPid = crawlInternalPid(product);
            String productUrl = crawlProductUrl(internalPid, pricesJson);

            String name = product.optString("name");
            String image = crawlImage(product);
            List<JSONObject> variations = crawlvariation(product);
            try {
               for(JSONObject obj: variations){
                 int priceInCents = obj.optInt("bestPrice");
                  boolean isAvailable = crawlAvailability(obj);
                  internalId = crawlInternalId(obj, internalPid);
                  String nameProduct = variationName(name);
                  RankingProduct rankingProduct = RankingProductBuilder.create()
                     .setUrl(productUrl)
                     .setInternalId(internalId)
                     .setInternalPid(internalPid)
                     .setName(nameProduct)
                     .setImageUrl(image)
                     .setPosition(position)
                     .setPriceInCents(priceInCents)
                     .setAvailability(isAvailable)
                     .build();

                  saveDataProduct(rankingProduct);
               }
               position++;

            } catch (NullPointerException e) {
               this.log(e.getMessage());
            }
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


   protected List<JSONObject> crawlvariation(JSONObject product) {
      try {
         for(Object object:  pricesJson){

            JSONObject obj = (JSONObject) object;
            JSONArray variations = obj.optJSONArray("sku_variations");
            if(!variations.isEmpty() && internalPid.equals(obj.optString("productId"))) {
               List<JSONObject> matchedVariations = IntStream
                  .range(0, variations.length())
                  .mapToObj(variations::optJSONObject)
                  .collect(Collectors.toList());
               if(!matchedVariations.isEmpty()) {
                  return matchedVariations;
               }

            }
         }

      } catch (NullPointerException pointer) {
         price = 0;
         List<JSONObject> listVoid = null;
         return listVoid;
      }
      List<JSONObject> listVoid = null;
      return listVoid;
   }


   @Override
   protected boolean crawlAvailability(JSONObject product) {
      try {
         return product.optBoolean("available");
      }catch (NullPointerException ex){
         return false;
      }

   }
   @Override
   protected String crawlInternalId(JSONObject product, String internalPid) {
      try{
         return product.optString("sku");
      }catch (NullPointerException ex){
         return "";
      }

   }
   protected String variationName(String name){
      try {
         String[] idSplit = internalId.split("C");
         return  name+ " caixa com " + idSplit[1];

      }catch (ArrayIndexOutOfBoundsException ex){
         return name;
      }
   }

   protected JSONArray fetchPrice(String id) {
      String url = "https://www.compra-agora.com/api/productLookup/" + id;
      Map<String, String> headers = new HashMap<>();
      headers.put("Cookie", session.getOptions().optString("cookie"));

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build();

      Response response = dataFetcher.get(session, request);

      return CrawlerUtils.stringToJsonArray(response.getBody());
   }

   protected String crawlProductUrl(String id, JSONArray productsApi) {
      String url = "";
      for (Object product : productsApi) {
         Number productIdTest = JSONUtils.getValueRecursive(product, "productId", Number.class);

         if (productIdTest != null) {
            if (Objects.equals(productIdTest.toString(), id)) {
               url = JSONUtils.getValueRecursive(product, "product_url", String.class);
            }
         }
      }

      if (url.startsWith("//")) {
         url = "https:" + url;
      } else if (url.startsWith("www.")) {
         url = "https://" + url;
      } else if(!url.startsWith(this.homePage)) {
         url = this.homePage + url;
      }

      return url;
   }
   protected String mountUrlid(JSONArray products){
      String url="";
      for (Object object : products) {
         JSONObject product = (JSONObject) object;
         url = url + "+" +crawlInternalPid(product);
      }
      return url;
   }
}
