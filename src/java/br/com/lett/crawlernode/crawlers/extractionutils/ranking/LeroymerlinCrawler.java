package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.LettProxy;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class LeroymerlinCrawler extends CrawlerRankingKeywords {

   protected String region;

   public LeroymerlinCrawler(Session session) {
      super(session);
      this.region = getRegion();
      super.fetchMode = FetchMode.APACHE;
   }

   protected String getRegion() {
      return session.getOptions().optString("region", "");
   }

   @Override
   protected void extractProductsFromCurrentPage() throws Exception {
      JSONObject data = fetchPage();
      JSONArray products = data.optJSONArray("products");

      if (products != null && !products.isEmpty()) {
         int totalProducts = JSONUtils.getValueRecursive(data, "metadata.totalCount", ".", Integer.class, 0);
         setTotalProducts(totalProducts);
         for (Object object : products) {
            JSONObject product = (JSONObject) object;
            String productUrl = crawlUrl(product);
            String internalId = product.optString("id");
            String name = product.optString("name");
            String image = product.optString("picture");
            int priceInCents = crawlPrice(product);
            boolean isAvailable = !product.optBoolean("isSoldOut");


            try {
               RankingProduct rankingProduct = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(internalId)
                  .setInternalPid(null)
                  .setName(name)
                  .setImageUrl(image)
                  .setPriceInCents(priceInCents)
                  .setAvailability(isAvailable)
                  .build();

               saveDataProduct(rankingProduct);
            } catch (MalformedProductException e) {
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

   public LettProxy getFixedIp() throws IOException {

      LettProxy lettProxy = new LettProxy();
      lettProxy.setSource("fixed_ip");
      lettProxy.setPort(3144);
      lettProxy.setAddress("haproxy.lett.global");
      lettProxy.setLocation("brazil");

      return lettProxy;
   }

   private String crawlUrl(JSONObject product) {
      String url = product.optString("url");
      if (!url.isEmpty()) {
         return url + "?region=" + this.region;
      } else {
         String id = product.optString("id");
         String name = product.optString("name");
         return CommonMethods.toSlug(name) + "_" + id + "?region=" + this.region;
      }
   }

   protected JSONObject fetchPage() throws Exception {
      String hash = getHashCategory();
      String url = "https://www.leroymerlin.com.br/api/boitata/v1/categories/" + hash + "/products?perPage=36&term=" + this.keywordWithoutAccents.replaceAll(" ", "%20") + "&searchTerm=" + this.keywordWithoutAccents.replaceAll(" ", "%20") + "&searchType=Shortcut&page=" + this.currentPage;

      if (hash == null) {
         url = "https://www.leroymerlin.com.br/api/boitata/v1/search?term=fresas&searchTerm=fresas&searchType=default";
      }
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setProxy(
            getFixedIp()
         )
         .build();

      Response response = dataFetcher.get(session, request);

      return CrawlerUtils.stringToJSONObject(response.getBody());
   }

   protected void setTotalProducts(int totalProducts) {
      this.totalProducts = Math.min(totalProducts, 300);
   }

   private int crawlPrice(JSONObject product) {
      int price = 0;
      Object priceData = product.optQuery("/price/to");
      if (priceData instanceof JSONObject) {
         JSONObject priceObject = (JSONObject) priceData;
         Double integers = JSONUtils.getDoubleValueFromJSON(priceObject, "integers", false);
         if (integers == null) integers = 0.0;
         String decimals = priceObject.optString("decimals", "0");
         price = (int) (Math.round(integers * 100) + Integer.parseInt(decimals));
      }

      return price;
   }

   private String getHashCategory() {
      Document document;
      String url = "https://www.leroymerlin.com.br/search?term=" + this.keywordEncoded.replace(" ", "%20") + "&searchTerm=" + this.keywordEncoded.replace(" ", "%20") + "&searchType=default";
      try {
         HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
         HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(url))
            .build();
         HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
         document = Jsoup.parse(response.body());
      } catch (Exception e) {
         throw new RuntimeException("Failed in scrape document: " + session.getOriginalURL(), e);
      }

      return CrawlerUtils.scrapStringSimpleInfoByAttribute(document, "body > span[data-component=\"rich-relevance/category-tracking\"]", "data-category-id");
   }
}
