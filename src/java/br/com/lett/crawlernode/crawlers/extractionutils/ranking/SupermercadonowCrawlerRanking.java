package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import java.util.HashMap;
import java.util.Map;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class SupermercadonowCrawlerRanking extends CrawlerRankingKeywords {

   public SupermercadonowCrawlerRanking(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   protected String loadUrl = getLoadUrl();

   protected String getLoadUrl() {
      return session.getOptions().optString("getLoadUrl");
   }

   protected String host = getHost();
   private final String homePage = "https://" + host + "/produtos/" + loadUrl + "/";
   private Map<String, String> headers = new HashMap<>();

   @Override
   protected void processBeforeFetch() {
      super.processBeforeFetch();
      Request request = RequestBuilder.create().setUrl(homePage).setCookies(cookies).setHeaders(headers).build();
      Response response = this.dataFetcher.get(session, request);

      headers.put("Accept", "text/json");
      headers.put("X-SNW-Token", "XLBhhbP1YEkB2tL61wkX163Dqm9iIDpx");

      this.cookies = response.getCookies();
   }

   protected String getHost() {
      return "supermercadonow.com";
   }


   @Override
   public void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 50;

      this.log("Página " + this.currentPage);
      JSONObject search = crawlSearchApi();

      if (search.has("items") && search.getJSONArray("items").length() > 0) {
         JSONArray products = search.getJSONArray("items");

         if (this.totalProducts == 0) {
            this.totalProducts = products.length();
            this.log("Total da busca: " + this.totalProducts);
         }

         for (int i = 0; i < products.length(); i++) {
            JSONObject product = products.getJSONObject(i);

            String productUrl = crawlProductUrl(product);
            String internalPid = crawlInternalPid(product);
            String internalId = crawlInternalId(product);
            String name = product.optString("name");
            String image = product.optString("image_thumbnail");
            int price = crawlPrice(product);
            boolean available = product.optBoolean("in_stock");

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setImageUrl(image)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(available)
               .build();

            saveDataProduct(productRanking);

            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }
      } else if (currentPage != 1) {
         this.result = false;
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

   }

   protected int crawlPrice(JSONObject product) {
      int priceInCents = 0;
      Object price = product.opt("price");
      if (price instanceof Double) {
         priceInCents = (int) Math.round((Double) price * 100);
      } else if (price instanceof Integer) {
         priceInCents = (int) price * 100;
      }
      return priceInCents;
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

   private String crawlInternalId(JSONObject json) {
      String internalId = null;

      if (json.has("product_store_id")) {
         internalId = json.get("product_store_id").toString();
      }

      return internalId;
   }

   private String crawlInternalPid(JSONObject json) {
      String internalPid = null;

      if (json.has("slug")) {
         internalPid = json.get("slug").toString().split("-")[0];
      }

      return internalPid;
   }

   private String crawlProductUrl(JSONObject product) {
      String urlProduct = null;

      if (product.has("slug")) {
         urlProduct = homePage + "produto/" + product.getString("slug");
      }

      return urlProduct;
   }

   private JSONObject crawlSearchApi() {
      String url = "https://api.supermercadonow.com/search/v1/bulksearch?query=" + this.keywordWithoutAccents.replace(" ", "%20") + "&stores=" + loadUrl + "&size=" + this.pageSize + "&page=" + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);
      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).build();

      return CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
   }

   @Override
   protected boolean hasNextPage() {
      return true;
   }
}
