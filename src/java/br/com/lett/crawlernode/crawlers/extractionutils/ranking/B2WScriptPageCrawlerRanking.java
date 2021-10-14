package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
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
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Iterator;

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

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .mustSendContentEncoding(false)
         .setFetcheroptions(
            FetcherOptions.FetcherOptionsBuilder.create()
               .mustUseMovingAverage(false)
               .mustRetrieveStatistics(true)
               .build()
         ).setProxyservice(
            Arrays.asList(
               ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY
            )
         ).build();


      Response response = new JsoupDataFetcher().get(session, request);
      String content = response.getBody();

      int statusCode = response.getLastStatusCode();

      if ((Integer.toString(statusCode).charAt(0) != '2' &&
         Integer.toString(statusCode).charAt(0) != '3'
         && statusCode != 404)) {
         request.setProxyServices(Arrays.asList(
            ProxyCollection.BUY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR));

         content = new FetcherDataFetcher().get(session, request).getBody();
      }

      return Jsoup.parse(content);

   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 24;
      this.log("Página " + this.currentPage);

      Document doc = fetchPage();
      JSONObject json = selectJsonFromHtml(doc);

      JSONObject search = json != null ? getProducts(json) : null;
      JSONArray products = search != null ? search.optJSONArray("products") : null;

      if (products != null && !products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts(search);
         }
         for (Object e : products) {
            if (e instanceof JSONObject) {
               JSONObject productJson = (JSONObject) e;
               JSONObject productInfo = productJson.optJSONObject("product");

               if (productInfo != null && !productInfo.isEmpty()) {

                  String internalId = JSONUtils.getValueRecursive(productInfo, "offers.result.0.sku", String.class);
                  String internalPid = productInfo.optString("id");
                  String productUrl = homePage + "produto/" + internalPid;
                  String name = productInfo.optString("name");
                  String imageUrl = JSONUtils.getValueRecursive(productJson, "images.0.large", String.class);
                  int price = CommonMethods.doublePriceToIntegerPrice(JSONUtils.getValueRecursive(productInfo, "offers.result.0.salesPrice", Double.class), 0);
                  boolean isAvailable = price != 0;

                  //New way to send products to save data product
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


   public JSONObject selectJsonFromHtml(Document doc) throws JSONException, ArrayIndexOutOfBoundsException, IllegalArgumentException, UnsupportedEncodingException {
      JSONObject jsonObject = new JSONObject();
      Elements scripts = doc.select("body > script");

      for (Element e : scripts) {
         String script = e.html();
         if (script.contains("window.__PRELOADED_STATE__ =")) {
            String readyToDecode = script.replace("%", "%25");
            String decode = URLDecoder.decode(readyToDecode, "UTF-8");
            String split = CrawlerUtils.getStringBetween(decode, "window.__PRELOADED_STATE__ =", ",\"session\":") + "}";
            jsonObject = CrawlerUtils.stringToJson(split);
            break;
         }
      }

      return jsonObject;
   }

   private JSONObject getProducts(JSONObject json) {
      JSONObject search = new JSONObject();
      JSONObject pages = json.optJSONObject("pages");
      if (pages != null) {
         Iterator<String> keys = pages.keys();
         String currentKey;
         while (keys.hasNext()) {
            currentKey = keys.next();
            JSONObject valueContent = pages.optJSONObject(currentKey);
            if (valueContent != null && valueContent.has("queries")) {
               search = JSONUtils.getValueRecursive(valueContent, "queries.pageSearch.result.search", JSONObject.class);
               break;
            }
         }
      }
      return search;
   }

   private void setTotalProducts(JSONObject json) {
      this.totalProducts = JSONUtils.getValueRecursive(json, "total", Integer.class);
      this.log("Total da busca: " + this.totalProducts);
   }
}
