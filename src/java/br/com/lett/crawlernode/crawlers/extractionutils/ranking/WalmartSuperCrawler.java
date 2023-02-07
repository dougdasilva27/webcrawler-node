package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

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
import org.apache.http.HttpHeaders;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WalmartSuperCrawler extends CrawlerRankingKeywords {

   public WalmartSuperCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.JSOUP;
   }

   String store_id = session.getOptions().optString("store_id");

   private JSONObject getJsonFromHtml(Document doc) {
      String script = CrawlerUtils.scrapScriptFromHtml(doc, "#__NEXT_DATA__");
      JSONArray scriptArray = JSONUtils.stringToJsonArray(script);
      Object json = scriptArray.get(0);
      JSONObject jsonObject = (JSONObject) json;
      return JSONUtils.getValueRecursive(jsonObject, "props.pageProps.initialData.searchResult.itemStacks.0", JSONObject.class, new JSONObject());
   }

   private JSONObject fetchJSONArray(String url) {
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setProxyservice(
            Arrays.asList(
               ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_DE_HAPROXY))
         .build();
      Response response = CrawlerUtils.retryRequest(request, session, new JsoupDataFetcher(), true);
      return getJsonFromHtml(Jsoup.parse(response.getBody()));
   }

   @Override
   public void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 44;
      this.log("Página " + this.currentPage);
//      String url = "https://super.walmart.com.mx/api/assembler/v2/page/search?Ntt=" + this.keywordEncoded + "&No=" + (this.currentPage - 1) +
//         "&Nrpp=10&storeId=" + store_id + "&profileId=NA";
      String url = "https://super.walmart.com.mx/search?q=" + this.keywordEncoded + "&page=" + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);

      JSONObject search = fetchJSONArray(url);
      if (this.totalProducts == 0) {
         this.totalProducts = search.optInt("count", 0);
      }
      JSONArray products = search.optJSONArray("items");


      if (products != null && !products.isEmpty()) {

         for (Object o : products) {
            if (o instanceof JSONObject) {
               JSONObject product = (JSONObject) o;
               String productUrl = getOriginalUrl(product);
               String internalId = product.optString("usItemId");
               String name = product.optString("name");
               String imageUrl = product.optString("image");
               int price = product.optInt("price", 0) * 100;
               boolean isAvailable = product.optString("availabilityStatusDisplayValue", "").equals("In stock");

               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(internalId)
                  .setName(name)
                  .setPriceInCents(price)
                  .setAvailability(isAvailable)
                  .setImageUrl(imageUrl)
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

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

   }

   private String getOriginalUrl(JSONObject productJson) {
      String suffixUrl = productJson.optString("canonicalUrl");
      if (suffixUrl != null && !suffixUrl.isEmpty()) {
         return "https://super.walmart.com.mx" + suffixUrl;
      }
      return null;
   }
   //   private JSONObject fetchJSONApi(String url) {
//
//      String referer = "https://super.walmart.com.mx/productos?Ntt=" + this.keywordEncoded;
//
//      Map<String, String> headers = new HashMap<>();
//      headers.put(HttpHeaders.HOST, "super.walmart.com.mx");
//      headers.put(HttpHeaders.CONNECTION, "keep-alive");
//      headers.put("x-dtreferer", referer);
//      headers.put(HttpHeaders.ACCEPT, "application/json");
//      headers.put(HttpHeaders.CONTENT_TYPE, "application/json");
//      headers.put(HttpHeaders.REFERER, referer);
//      headers.put(HttpHeaders.ACCEPT_ENCODING, "");
//      headers.put(HttpHeaders.ACCEPT_LANGUAGE, "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");
//      headers.put(HttpHeaders.CACHE_CONTROL, "no-cache");
//
//      Request request = Request.RequestBuilder.create()
//         .setUrl(url)
//         .setCookies(cookies)
//         .setHeaders(headers)
//         .setProxyservice(Arrays.asList(
//            ProxyCollection.NETNUT_RESIDENTIAL_DE_HAPROXY,
//            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
//            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
//            ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
//            ProxyCollection.NETNUT_RESIDENTIAL_ANY_HAPROXY,
//            ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY))
//         .mustSendContentEncoding(false)
//         .build();
//
//      String response = CrawlerUtils.retryRequestString(request, List.of(new JsoupDataFetcher(), new ApacheDataFetcher(), new FetcherDataFetcher()), session);
//      return CrawlerUtils.stringToJson(response);
//   }


}
