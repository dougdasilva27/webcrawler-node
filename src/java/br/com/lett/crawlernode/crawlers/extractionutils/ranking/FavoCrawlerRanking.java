package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.crawler.DiscoveryCrawlerSession;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class FavoCrawlerRanking extends CrawlerRankingKeywords {
   public FavoCrawlerRanking(Session session) {
      super(session);
      super.fetchMode = FetchMode.JSOUP;
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
      String url = "https://customer-bff.favoapp.com.br/products/textsearch?tienda=" + this.STORE + "&pag=" + this.currentPage + "&filter=" + this.keywordWithoutAccents.replaceAll(" ", "%20");
      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");
      headers.put("x-origin-id", this.ORIGIN_ID);
      headers.put("referer", "https://" + STORE + ".mercadofavo.com/");
      headers.put("origin", "https://" + STORE + ".mercadofavo.com/");
      headers.put("accept", "application/json, text/plain, */*");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY))
         .setHeaders(headers)
         .build();
      return CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody())
         ;
   }

   private String scrapImageUrl(JSONObject productJson) {
      String image = null;
      JSONObject images = productJson.optJSONObject("imagen");

      if (images != null) {
         image = images.optString("size-480");
      }

      return image;
   }

   private String scrapUrl(JSONObject product) {
      String productURL = "https://" + this.ORIGIN_ID + ".mercadofavo.com/" + this.STORE + "/product/";
      String slug = product.optString("slug");
      return productURL + slug;
   }

   @Override
   protected boolean hasNextPage() {
      return true;
   }
}
