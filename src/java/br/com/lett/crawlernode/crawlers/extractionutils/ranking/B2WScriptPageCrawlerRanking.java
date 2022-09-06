package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
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

import java.io.UnsupportedEncodingException;
import java.util.*;

public abstract class B2WScriptPageCrawlerRanking extends CrawlerRankingKeywords {

   protected B2WScriptPageCrawlerRanking(Session session) {
      super(session);
   }

   protected final String homePage = getHomePage();

   protected abstract String getHomePage();

   protected Document fetchPage() {
      String keyword = this.keywordWithoutAccents.replace(" ", "-");

      String url = homePage + "busca/" + keyword + "?limit=24&offset=" + (this.currentPage - 1) * pageSize;
      this.log("Link onde são feitos os crawlers: " + url);

      Map<String, String> headers = new HashMap<>();

      headers.put("authority", homePage.replace("https://", ""));
      headers.put("sec-ch-ua", " \" Not A;Brand\";v=\"99\", \"Chromium\";v=\"90\", \"Google Chrome\";v=\"90\"");
      headers.put("sec-ch-ua-mobile", "?0");
      headers.put("upgrade-insecure-requests", "1");
      headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,/;q=0.8,application/signed-exchange;v=b3;q=0.9");
      headers.put("sec-fetch-site", "none");
      headers.put("sec-fetch-mode", "navigate");
      headers.put("sec-fetch-user", "?1");
      headers.put("sec-fetch-dest", "document");
      headers.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.212 Safari/537.36");
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(this.cookies)
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
               ProxyCollection.NETNUT_RESIDENTIAL_BR,
               ProxyCollection.NETNUT_RESIDENTIAL_MX,
               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_DE_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY
            )
         )
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher(), new FetcherDataFetcher()), session);

      return Jsoup.parse(response.getBody());

   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 24;
      this.log("Página " + this.currentPage);

      this.currentDoc = fetchPage();
      JSONObject json = CrawlerUtils.selectJsonFromHtml(this.currentDoc, "body > script", "window.__APOLLO_STATE__ =", null, false, false);
      JSONObject rootQuery = json.optJSONObject("ROOT_QUERY");
      JSONObject productsJson = getJson(rootQuery, "search");
      JSONArray products = productsJson.optJSONArray("products");

      if (products != null && !products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts(productsJson);
         }
         for (Object e : products) {
            if (e instanceof JSONObject) {
               JSONObject productJson = (JSONObject) e;
               JSONObject productInfo = productJson.optJSONObject("product");

               if (productInfo != null && !productInfo.isEmpty()) {
                  JSONObject offers = getJson(productInfo, "offers");
                  String internalId = offers != null ? JSONUtils.getValueRecursive(offers, "result.0.sku", String.class) : null;
                  String internalPid = productInfo.optString("id");
                  String productUrl = homePage + "produto/" + internalPid;
                  String name = productInfo.optString("name");
                  JSONArray imageJson = getJsonArray(productInfo);
                  String imageUrl = imageJson != null ? JSONUtils.getValueRecursive(imageJson, "0.large", String.class) : null;
                  int price = offers != null ? CommonMethods.doublePriceToIntegerPrice(JSONUtils.getValueRecursive(offers, "result.0.salesPrice", Double.class), 0) : null;
                  boolean isAvailable = price != 0;

                  RankingProduct productRanking = RankingProductBuilder.create()
                     .setUrl(productUrl)
                     .setInternalId(internalId)
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
            }

         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");

   }


   private JSONObject getJson(JSONObject jsonObject, String type) {
      if (jsonObject != null) {
         for (Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
            String key = it.next();
            if (key.contains(type)) {
               return jsonObject.optJSONObject(key);
            }
         }
      }

      return new JSONObject();

   }

   private JSONArray getJsonArray(JSONObject jsonObject) {
      for (Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
         String key = it.next();
         if (key.contains("image")) {
            return jsonObject.optJSONArray(key);
         }

      }
      return new JSONArray();

   }

   protected void setTotalProducts(JSONObject json) {
      this.totalProducts = json.optInt("total");
      this.log("Total da busca: " + this.totalProducts);
   }
}
