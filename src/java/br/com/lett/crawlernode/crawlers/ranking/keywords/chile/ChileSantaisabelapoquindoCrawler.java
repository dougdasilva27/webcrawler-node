package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
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
import java.util.Map;

public class ChileSantaisabelapoquindoCrawler extends CrawlerRankingKeywords {

   public ChileSantaisabelapoquindoCrawler(Session session) {
      super(session);
     // super.fetchMode = FetchMode.JSOUP;
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.log("Página " + this.currentPage);

      this.pageSize = 40;

      JSONObject searchedJson = getJSONFromAPI();

      JSONArray products = JSONUtils.getValueRecursive(searchedJson, "products", JSONArray.class);

      if (products != null && products.length() > 0) {

         if (this.totalProducts == 0) {

            setTotalProducts(searchedJson);

         }

         for (Object object : products) {

            JSONObject product = (JSONObject) object;

            String internalId = JSONUtils.getValueRecursive(product, "items.0.itemId", String.class);;
            String productUrl = CrawlerUtils.completeUrl(product.optString("linkText"), "https", "www.santaisabel.cl") + "/p";
            String name = product.optString("productName");
            String imgUrl = JSONUtils.getValueRecursive(product, "items.0.images.0.imageUrl", String.class);
            Boolean isAvailable = JSONUtils.getValueRecursive(product, "items.0.sellers.0.commertialOffer.AvailableQuantity", Integer.class) > 0;
            Integer price = isAvailable ? JSONUtils.getValueRecursive(product, "items.0.sellers.0.commertialOffer.Price", Integer.class) : null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setName(name)
               .setImageUrl(imgUrl)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
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


   private JSONObject getJSONFromAPI() {
      String urlAPI = "https://apis.santaisabel.cl:8443/catalog/api/v1/pedrofontova/search/" + this.keywordWithoutAccents + "?page=" + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + urlAPI);

      Map<String, String> headers = new HashMap<>();
      headers.put("x-api-key", "5CIqbUOvJhdpZp4bIE5jpiuFY3kLdq2z");
      headers.put("content-type", "application/json");
      headers.put("accept", "*/*");
      headers.put("x-consumer", "santaisabel");
      headers.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.182 Safari/537.36");
      headers.put("Connection", "keep-alive");
      headers.put("authority", "apis.santaisabel.cl:8443");
      headers.put("x-account", "pedrofontova");
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");

      Request request = Request.RequestBuilder.create()
         .setUrl(urlAPI)
         .setCookies(cookies)
         .mustSendContentEncoding(false)
         .setHeaders(headers)
         .setFetcheroptions(
            FetcherOptions.FetcherOptionsBuilder.create()
               .mustUseMovingAverage(false)
               .mustRetrieveStatistics(true)
               .setForbiddenCssSelector("#px-captcha")
               .build()
         ).setProxyservice(
            Arrays.asList(
               ProxyCollection.BUY_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_BR
            )
         ).build();


      Response response = new FetcherDataFetcher().post(session, request);
      String content = response.getBody();


      return CrawlerUtils.stringToJson(content);

   }

   protected void setTotalProducts(JSONObject searchedJson) {
      this.totalProducts = searchedJson.optInt("recordsFiltered");
      this.log("Total da busca: " + this.totalProducts);

   }

}
