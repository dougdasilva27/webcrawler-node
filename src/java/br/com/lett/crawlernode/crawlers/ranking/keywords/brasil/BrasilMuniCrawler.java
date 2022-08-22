package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
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
import br.com.lett.crawlernode.util.Logging;
import com.google.gson.JsonParser;
import org.apache.http.HttpHeaders;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BrasilMuniCrawler extends CrawlerRankingKeywords {
   public static final String PRODUCTS_API_URL = "https://a60vkx00hn-dsn.algolia.net/1/indexes/*/queries?x-algolia-agent=Algolia%20for%20JavaScript%20(4.10.5)%3B%20Browser%20(lite)%3B%20JS%20Helper%20(3.10.0)%3B%20react%20(16.14.0)%3B%20react-instantsearch%20(6.12.1)&x-algolia-api-key=671ae10cfea41e8580445bd850aa9d9c&x-algolia-application-id=A60VKX00HN";

   public BrasilMuniCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.JSOUP;
   }

   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 20;
      this.log("Página " + this.currentPage);

      JSONObject search = fetchProductsFromAPI();
      JSONArray arraySkus = search.has("hits") ? search.getJSONArray("hits/") : new JSONArray();

      if (arraySkus.length() > 0) {

         if (this.totalProducts == 0) {
            setTotalProducts(search);
         }

         for (Object product : arraySkus) {
            JSONObject jsonSku = (JSONObject) product;
            String name = JSONUtils.getStringValue(jsonSku, "slug_name");
            String internalId = JSONUtils.getStringValue(jsonSku, "uuid");
            String productUrl = "https://shop.munitienda.com.br/BR-SAO/mp/" + internalId + "/" + name;
            String imgUrl = JSONUtils.getStringValue(jsonSku, "image");
            Integer price = crawlPrice(jsonSku);
            boolean isAvailable = JSONUtils.getIntegerValueFromJSON(jsonSku, "in_stock", 0) >= 1 ? true : false;
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

   private Integer crawlPrice(JSONObject product) {
      Integer price;
      String priceStr = product.optQuery("/price/value").toString();
      if (priceStr == null) {
         priceStr = product.optQuery("/price/market_price").toString();
      }
      Double priceDouble = Double.parseDouble(priceStr) * 100;
      price = priceDouble.intValue();
      return price;
   }

   private void setTotalProducts(JSONObject search) {
      this.totalProducts = JSONUtils.getIntegerValueFromJSON(search, "nbHits", 0);
      this.log("Total: " + this.totalProducts);
   }

   private JSONObject fetchProductsFromAPI() {
      JSONObject products = new JSONObject();

      String payload = "{\"requests\":[{\"indexName\":\"BR-SAO__product_index__br-production\",\"params\":\"clickAnalytics=true&facets=%5B%5D&highlightPostTag=%3C%2Fais-highlight-0000000000%3E&highlightPreTag=%3Cais-highlight-0000000000%3E&page=" + (currentPage - 1) + "&query=" + keywordEncoded + "&tagFilters=\"}]}";

      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
      headers.put("Origin", "https://shop.munitienda.com.br/");
      headers.put("Connection", "keep-alive");
      headers.put("Referer", "https://shop.munitienda.com.br/");
      headers.put("accept", "*/*");
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");
      headers.put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.88 Safari/537.36");

      Request request = Request.RequestBuilder.create()
         .setUrl(PRODUCTS_API_URL)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_ES
         ))
         .setPayload(payload)
         .mustSendContentEncoding(true)
         .build();
      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher(), new FetcherDataFetcher()), session);

      return CrawlerUtils.stringToJson(response.getBody());
   }
}
