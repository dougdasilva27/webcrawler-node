package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChileJumboCrawler extends CrawlerRankingKeywords {

   public ChileJumboCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.APACHE;
   }

   protected String storeCode = getStoreCode();
   protected static final String API_KEY = "IuimuMneIKJd3tapno2Ag1c1WcAES97j";
   protected static final String HOST = br.com.lett.crawlernode.crawlers.extractionutils.core.ChileJumboCrawler.HOST;

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.log("Página " + this.currentPage);

      this.pageSize = 40;

      JSONObject bodyJson = fetchProducts();

      if (this.currentPage == 1) {
         this.totalProducts = bodyJson.optInt("recordsFiltered");
      }

      JSONArray products = JSONUtils.getJSONArrayValue(bodyJson, "products");

      if (!products.isEmpty()) {
         for (Object o : products) {
            JSONObject product = (JSONObject) o;
            String internalPid = product.optString("productReference", null);
            String productUrl = crawlProductUrl(product);
            String productName = scrapName(product);
            Object itemObject = product.optQuery("/items/0");

            if (itemObject instanceof JSONObject) {
               JSONObject item = (JSONObject) itemObject;
               Integer price = scrapPrice(item);
               String imageUrl = scrapImage(item);

               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalPid(internalPid)
                  .setName(productName)
                  .setPriceInCents(price)
                  .setAvailability(price != null)
                  .setImageUrl(imageUrl)
                  .build();

               saveDataProduct(productRanking);
            }

            if (this.arrayProducts.size() == productsLimit)
               break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
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

   private JSONObject fetchProducts() {
      String url = "https://apijumboweb.smdigital.cl/catalog/api/v1/search/" + keywordWithoutAccents.toLowerCase().replace(" ", "%20") + "?page=" + this.currentPage;

                  //https://apijumboweb.smdigital.cl/catalog/api/v1/search/afeitadora?page=1
      Map<String, String> headers = new HashMap<>();
      headers.put("x-api-key", API_KEY);
      headers.put("authority", "apijumboweb.smdigital.cl");
      headers.put("content-type", "application/json");
      headers.put("origin", "https://www.jumbo.cl");
      headers.put("referer", "https://www.jumbo.cl/");
      headers.put("x-requested-with", "XMLHttpRequest");

      String payload = "{\"selectedFacets\":[{\"key\":\"trade-policy\",\"value\":\"" + getStoreCode() + "\"}]}";

      Request request = RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload)
         .setProxyservice(
            Arrays.asList(
               ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_ES,
               ProxyCollection.NETNUT_RESIDENTIAL_BR
            )
         )
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new JsoupDataFetcher(), new FetcherDataFetcher()), session, "post");
      return CrawlerUtils.stringToJson(response.getBody());
   }

   protected String getStoreCode() {
      return session.getOptions().optString("code_locale");
   }

   private String crawlProductUrl(JSONObject product) {
      String productUrl = product.optString("linkText", null);

      if (productUrl != null) {
         productUrl += productUrl.endsWith("/p") ? "" : "/p";
      }

      return CrawlerUtils.completeUrl(productUrl, "https", HOST);
   }
}
