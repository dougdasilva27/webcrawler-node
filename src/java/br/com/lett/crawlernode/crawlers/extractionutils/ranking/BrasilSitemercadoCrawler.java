package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BrasilSitemercadoCrawler extends CrawlerRankingKeywords {

   private String homePage = getHomePage();

   protected String getHomePage() {
      return session.getOptions().optString("url");
   }

   private final String API_URL = session.getOptions().optString("api_url", "https://ecommerce-backend-wl.sitemercado.com.br/api/b2c/product?store_id=");
   private final String HOST_URL = session.getOptions().optString("host_url", "www.sitemercado.com.br");
   private Map<String, Integer> lojaInfo = getLojaInfo();

   private final Double latitude = session.getOptions().optDouble("latitude");
   private final Double longitude = session.getOptions().optDouble("longitude");

   public BrasilSitemercadoCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.JSOUP;
   }

   protected Map<String, Integer> getLojaInfo() {
      Map<String, Integer> storeMap = new HashMap<>();
      storeMap.put("IdLoja", session.getOptions().optInt("idLoja"));
      storeMap.put("IdRede", session.getOptions().optInt("idRede"));
      return storeMap;
   }

   protected String getLoadPayload() {
      JSONObject payload = new JSONObject();
      String[] split = homePage.split("/");

      payload.put("lojaUrl", CommonMethods.getLast(split));
      payload.put("redeUrl", split[split.length - 2]);

      return payload.toString();
   }

   protected String apiSearchUrl(String lojaId) {
      return API_URL + lojaId + "&text=";
   }

   @Override
   public void extractProductsFromCurrentPage() throws MalformedProductException {
      this.log("Página " + this.currentPage);
      JSONObject search = crawlProductInfo();

      if (search.has("products") && search.getJSONArray("products").length() > 0) {
         JSONArray products = search.getJSONArray("products");

         this.totalProducts = products.length();
         this.log("Total da busca: " + this.totalProducts);

         for (int i = 0; i < products.length(); i++) {
            JSONObject product = products.getJSONObject(i);

            String productUrl = crawlProductUrl(product);
            String internalPid = crawlInternalPid(product);
            String name = product.optString("excerpt");
            String imageUrl = CrawlerUtils.completeUrl(product.optString("imageFull"), "https", "");
            int price = CommonMethods.doublePriceToIntegerPrice(JSONUtils.getValueRecursive(product, "prices.0.price", Double.class), 0);
            boolean isAvailable = price != 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(null)
               .setInternalPid(internalPid)
               .setName(name)
               .setPriceInCents(price)
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

   @Override
   protected boolean hasNextPage() {
      return false;
   }

   private String crawlInternalPid(JSONObject product) {
      String internalPid = null;

      if (product.has("idProduct")) {
         internalPid = product.get("idProduct").toString();
      }

      return internalPid;
   }

   private String crawlProductUrl(JSONObject product) {
      String productUrl = null;

      if (product.has("url")) {
         productUrl = product.getString("url");

         if (!productUrl.contains("sitemercado")) {
            productUrl = (this.homePage + "/" + productUrl).replace("//produto/", "/produto/");

            // This can be "tirandentes/" or "stamatis/"
            String lastUrlPart = CommonMethods.getLast(this.homePage.split("-"));

            if (productUrl.contains(lastUrlPart + "/") && !productUrl.contains("produto")) {
               productUrl = productUrl.replace(lastUrlPart + "/", lastUrlPart + "/produto/");
            }
         }
      }

      return productUrl;
   }

   protected JSONObject crawlProductInfo() {
      String apiUrl = apiSearchUrl(lojaInfo.get("IdLoja").toString()) + this.keywordEncoded.replace("+", "%20");

      Map<String, String> headers = new HashMap<>();
      headers.put("hosturl", HOST_URL);
      headers.put("Accept", "application/json, text/plain, */*");
      headers.put("sm-token", "{\"Location\":{\"Latitude\":" + latitude + ",\"Longitude\":" + longitude + "},\"IdLoja\":" + lojaInfo.get("IdLoja") + ",\"IdRede\":" + lojaInfo.get("IdRede") + "}");

      Request requestApi = RequestBuilder.create()
         .setUrl(apiUrl)
         .setProxyservice(List.of(ProxyCollection.BUY, ProxyCollection.NETNUT_RESIDENTIAL_BR, ProxyCollection.LUMINATI_RESIDENTIAL_BR))
         .setCookies(cookies)
         .setHeaders(headers)
         .build();

      return CrawlerUtils.stringToJson(this.dataFetcher.get(session, requestApi).getBody());
   }

}
