package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class FavoCrawlerRanking extends CrawlerRankingKeywords {
   public FavoCrawlerRanking(Session session) {
      super(session);
   }

   private final String STORE = getStore();
   private final String ORIGIN_ID = getOriginId();

   private String getOriginId() {
      return session.getOptions().optString("origin_id");
   }

   private String getStore() {
      return session.getOptions().optString("tienda");
   }

   @Override
   public void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 20;
      this.log("Página " + this.currentPage);

      JSONObject search = fetchProductsFromApi();
      Object productsObj = search.optQuery("/data/products");

      if (productsObj instanceof JSONArray && ((JSONArray) productsObj).length() > 0) {
         JSONArray products = (JSONArray) productsObj;

         for (int i = 0; i < products.length(); i++) {
            JSONObject product = products.optJSONObject(i);

            String productUrl = scrapUrl(product);
            String internalId = product.optString("sku");
            String internalPid = product.optString("_id");
            String name = product.optString("descripcion");
            String imageUrl = scrapImageUrl(product);
            Integer price = JSONUtils.getPriceInCents(product, "precio_aiyu");
            boolean available = product.optInt("stock", 0) > 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setImageUrl(imageUrl)
               .setPriceInCents(price)
               .setAvailability(available)
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

   private JSONObject fetchProductsFromApi() {
      String url = "https://customer-bff.favoapp.com.br/products/textsearch?tienda=" + this.STORE + "&pag=" + this.currentPage + "&filter=" + this.keywordEncoded.replaceAll(" ", "%20");
      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");
      headers.put("x-origin-id", this.ORIGIN_ID);

      Request request = Request.RequestBuilder.create().setUrl(url).setHeaders(headers).build();
      return CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
   }

   private String scrapImageUrl(JSONObject product) {
      String image = null;
      JSONObject images = product.optJSONObject("images");

      if (images != null) {
         if (images.has("size-1024")) {
            image = images.optString("size-1024");
         } else if (images.has("size-960")) {
            image = images.optString("size-960");
         } else if (images.has("size-480")) {
            image = images.optString("size-480");
         } else if (images.has("size-150")) {
            image = images.optString("size-150");
         }
      }

      return image;
   }

   private String scrapUrl(JSONObject product) {
      String searchURL = "https://" + this.ORIGIN_ID + ".mercadofavo.com/" + this.STORE + "?search=";
      String slug = product.optString("slug");
      return searchURL + slug;
   }
}
