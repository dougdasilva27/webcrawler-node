package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.*;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class BrasilFastshopCrawler extends CrawlerRankingKeywords {

   public BrasilFastshopCrawler(Session session) {
      super(session);
   }


   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.log("Página " + this.currentPage);

      String apiUrl = "https://fastshop-v6.neemu.com/searchapi/v3/search?apiKey=fastshop-v6&secretKey=7V0dpc8ZFxwCRyCROLZ8xA%253D%253D&terms="
         + this.keywordWithoutAccents.replace(" ", "%20") + "&resultsPerPage=9&page=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + apiUrl);
      Map<String, String> headers = new HashMap<>();
      headers.put("origin", "https://www.fastshop.com.br");

      String json = fetchGetFetcher(apiUrl, null, headers, null);
      JSONObject apiResponse = new JSONObject(json);

      if (apiResponse.optString("queryType").equals("redirect")) {
         apiResponse = scrapJsonByCategory(apiResponse);
      }

      extractProductFromJSON(apiResponse);

   }

   private void extractProductFromJSON(JSONObject api) throws MalformedProductException {
      if (api != null && api.has("products")) {
         JSONArray products = api.getJSONArray("products");

         if (this.totalProducts == 0) {
            this.totalProducts = products.length();
         }

         JSONArray prices = scrapPricesJson(products);
         for (int i = 0; i < products.length(); i++) {
            JSONObject product = products.getJSONObject(i);

            String internalPid = crawlinternalPid(product);
            String productUrl = crawlProductUrlFromJson(product, internalPid);
            List<String> internalIds = crawlInternalIds(product);
            String name = crawlName(product);
            String imgUrl = crawlImgUrl(product);
            Integer price = scrapPriceInCents(prices, internalPid, product);
            Boolean isAvailable = scrapAvailability(product);

            for (int j = 0; j < internalIds.size(); j++) {
               String internalId = internalIds.get(j);
               String variationName = isVoltageVariation(name, internalId, product);

               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(variationName)
                  .setImageUrl(imgUrl)
                  .setPriceInCents(price)
                  .setAvailability(isAvailable)
                  .build();

               saveDataProduct(productRanking);
            }

            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }
   }

   @Override
   protected boolean hasNextPage() {
      return true;
   }

   private JSONObject scrapJsonByCategory(JSONObject product) {
      String redirectUrl = product.optString("link");
      String categoryId = redirectUrl.replaceAll("[^0-9]", "");

      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/json");
      headers.put("origin", "https://www.fastshop.com.br");

      Request request = Request.RequestBuilder.create().setUrl("https://api.fastshop.com.br/regionalized-catalog/v1/catalog/by-category?category=" +
            categoryId
            +"&pageNumber=" + this.currentPage)
         .setHeaders(headers)
         .mustSendContentEncoding(false)
         .build();

      Response response = this.dataFetcher.get(session, request);

      JSONObject apiResponse = new JSONObject(response.getBody());

      return apiResponse;
   }

   private JSONArray scrapPricesJson(JSONArray products) {
      String internalPids = scrapAllInternalPid(products);
      JSONArray productsArray = new JSONArray();

      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/json");
      headers.put("origin", "https://www.fastshop.com.br");

      Request request = Request.RequestBuilder.create()
         .setUrl("https://price-management.fastshop.com.br/api/v1/price-promotion/price?store=fastshop&channel=webapp" + internalPids)
         .setHeaders(headers)
         .mustSendContentEncoding(false)
         .build();

      Response response = new JsoupDataFetcher().get(session, request);

      if (response.getBody() != null && response.getBody().startsWith("{")) {
         JSONObject apiResponse = new JSONObject(response.getBody());

         if (apiResponse.has("result")) {
            productsArray = JSONUtils.getValueRecursive(apiResponse, "result.0.products", JSONArray.class);
         }
      }

      return productsArray;
   }

   private String scrapAllInternalPid(JSONArray products) {
      String internalPids = "";

      if (products != null) {
         for (Object product : products) {
            if (product != null) {
               String pid = JSONUtils.getStringValue((JSONObject) product, "partNumber");
               if (pid != null) {
                  internalPids = internalPids.concat("&skus=" + pid);
               }
            }

         }
      }

      return internalPids;
   }

   private List<String> crawlInternalIds(JSONObject product) {
      List<String> internalIdList = new ArrayList();

      if (product != null && product.has("details")) {
         JSONObject details = product.getJSONObject("details");

         if (details.has("catalogEntryId")) {
            JSONArray internalIds = details.getJSONArray("catalogEntryId");

            for (int j = 0; j < internalIds.length(); j++) {
               if (internalIds.getString(j) != null) {
                  internalIdList.add(internalIds.getString(j));
               }
            }
         }
      } else if (product != null && product.has("productID")) {
         internalIdList = List.of(product.optString("productID"));
      }

      return internalIdList;
   }

   private String crawlinternalPid(JSONObject product) {
      String pid = null;

      if (product.has("id")) {
         pid = product.optString("id");
      }

      if (product.has("partNumber")) {
         pid = product.optString("partNumber");
      }

      return pid;
   }

   private String crawlProductUrlFromJson(JSONObject product, String pid) {
      StringBuilder productUrl = new StringBuilder();

      if (pid != null && product.has("url")) {
         productUrl.append("https://www.fastshop.com.br/web/p/d/");
         productUrl.append(pid + "/");
         productUrl.append(CommonMethods.getLast(product.get("url").toString().split("/")));
      } else if(product != null && product.has("partNumber")) {
         productUrl.append("https://www.fastshop.com.br/web/p/d/");
         productUrl.append(pid + "/");
         String url = convertNameToUrl(product.optString("shortDescription"));
         productUrl.append(url);
      } else {
         return null;
      }

      return productUrl.toString();
   }

   private String crawlImgUrl(JSONObject product) {
      String imgUrlUnformatted = JSONUtils.getValueRecursive(product, "images.default", String.class);

      if (imgUrlUnformatted != null) {
         imgUrlUnformatted = "https:" + imgUrlUnformatted;
      } else if (product.has("thumbnail")) {
         imgUrlUnformatted = "https://www.fastshop.com.br" + product.optString("thumbnail", null);
      }

      return imgUrlUnformatted;
   }

   private String convertNameToUrl(String name) {
      if (name != null) {
         name = name.toLowerCase();
         name = name.replaceAll("[^a-zA-Z0-9\\s]", "");
         name = name.replaceAll(" ", "-");
         return name;
      }
      return null;
   }

   private String crawlName(JSONObject product) {
      String name = null;

      if (product != null && product.has("name")) {
         name = product.optString("name", null);
      } else if (product != null && product.has("shortDescription")) {
         name = product.optString("shortDescription", null);
      }

      return name;
   }

   private Integer scrapPriceInCents(JSONArray prices, String internalPid, JSONObject product) {
      if (prices == null) {
         Double price = product.optDouble("price");
         Integer priceInCents = price.intValue() * 100;
         return priceInCents;
      } else {
         for (Object priceInfo : prices) {
            String pid = JSONUtils.getStringValue((JSONObject) priceInfo, "product");

            if (Objects.equals(internalPid, pid)) {
               Double productPrice = JSONUtils.getValueRecursive(priceInfo, "skus.0.promotions.0.value", Double.class);
               Double priceInCents = null;

               if (productPrice != null) {
                  priceInCents = productPrice * 100.0;
               } else {
                  productPrice = JSONUtils.getValueRecursive(priceInfo, "skus.0.price.offerPrice", Double.class);
                  if (productPrice != null) {
                     priceInCents = productPrice * 100.0;
                  }
               }
               if (priceInCents != null) {
                  return priceInCents.intValue();
               }
            }
         }
      }

      return null;
   }

   private Boolean scrapAvailability(JSONObject product) {
      Boolean available = false;

      if (product.has("status")) {
         available = product.optString("status").equals("AVAILABLE");
      } else if (product.has("buyable")) {
         available = product.optBoolean("buyable");
      }

      return available;
   }

   private String isVoltageVariation(String name, String internalId, JSONObject product) {
      String specsFirstProduct = JSONUtils.getValueRecursive(product, "skus.0.specs.Voltagem.0", String.class);
      String specsSecondProduct = JSONUtils.getValueRecursive(product, "skus.1.specs.Voltagem.0", String.class);

      if (specsFirstProduct != null && specsSecondProduct != null) {
         if (!specsFirstProduct.equals(specsSecondProduct)) {

            JSONArray skus = product.getJSONArray("skus");

            for (Object sku : skus) {
               String catalogId = JSONUtils.getValueRecursive(sku, "specs.catalogEntryId.0", String.class);

               if (Objects.equals(catalogId, internalId)) {
                  String voltage = JSONUtils.getValueRecursive(sku, "specs.Voltagem.0", String.class);

                  name = name + " - " + voltage;
               }
            }
         }
      }

      return name;
   }
}
