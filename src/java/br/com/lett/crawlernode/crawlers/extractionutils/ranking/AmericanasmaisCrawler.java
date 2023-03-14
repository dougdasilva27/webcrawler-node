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
import br.com.lett.crawlernode.crawlers.extractionutils.core.SaopauloB2WCrawlersUtils;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class AmericanasmaisCrawler extends CrawlerRankingKeywords {

   public AmericanasmaisCrawler(Session session) {
      super(session);
   }
   private final String storeId = getStoreId();
   private static final String HOME_PAGE = "https://www.americanas.com.br/lojas-proximas/33014556000196/";
   public String getStoreId() {
      return session.getOptions().optString("store_id");
   }
   private Document fetchPage() {

      String url = HOME_PAGE + storeId + "?ordenacao=topSelling&conteudo=" +
         this.keywordEncoded.replace("+", "%20") +
         "&limite=24&offset=" + (this.currentPage - 1) * pageSize;

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
               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY
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

      JSONObject apolloJson = CrawlerUtils.selectJsonFromHtml(doc, "script", "window.__APOLLO_STATE__ =", null, false, true);
      JSONObject json = extractProductFromApollo(apolloJson);
      JSONArray products = json.optJSONArray("products");

      if (!products.isEmpty()) {
         for (Object o : products) {
            if (o instanceof JSONObject) {
               JSONObject productJson = (JSONObject) o;
               JSONObject productInfo = productJson.optJSONObject("product");
               if (productInfo != null) {
                  String internalId = productInfo.optString("id");
                  String productUrl = HOME_PAGE + storeId + "/ship?ordenacao=relevance&conteudo=" + internalId;
                  String name = productInfo.optString("name");
                  String imgUrl = JSONUtils.getValueRecursive(productInfo, "images.0.large", String.class);
                  Integer price = CommonMethods.doublePriceToIntegerPrice(getPrice(apolloJson, productInfo), 0);
                  boolean isAvailable = price != 0;

                  RankingProduct productRanking = RankingProductBuilder.create()
                     .setUrl(productUrl)
                     .setInternalId(internalId)
                     .setInternalPid(internalId)
                     .setName(name)
                     .setPriceInCents(price)
                     .setImageUrl(imgUrl)
                     .setAvailability(isAvailable)
                     .build();

                  saveDataProduct(productRanking);
               }
            }
         }
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");

   }


   private static JSONObject extractProductFromApollo(JSONObject apollo) {
      JSONObject product = new JSONObject();

      JSONObject root = apollo.optJSONObject("ROOT_QUERY");
      if (root != null) {
         for (String key : root.keySet()) {
            if (key.startsWith("search")) {
               product = root.optJSONObject(key);
               break;
            }
         }
      }

      return product;
   }

   private String getKeyOffer(JSONObject productInfo) {
      JSONObject offers = SaopauloB2WCrawlersUtils.getJson(productInfo, "offers");
      return (String) offers.optQuery("/result/0/__ref");

   }

   private double getPrice(JSONObject apollo, JSONObject productInfo) {
      String keyOffers = getKeyOffer(productInfo);
      JSONObject offer = apollo.optJSONObject(keyOffers);
      return offer.optDouble("salesPrice", 0);
   }


   public JSONObject selectJsonFromHtml(Document doc) throws JSONException, ArrayIndexOutOfBoundsException, IllegalArgumentException, UnsupportedEncodingException {
      JSONObject jsonObject = new JSONObject();
      Elements scripts = doc.select("body > script");

      for (Element e : scripts) {
         String script = e.html();
         if (script.contains("window.__PRELOADED_STATE__ =")) {
            String split = CrawlerUtils.extractSpecificStringFromScript(script, "window.__PRELOADED_STATE__ = \"", true, "}", true)
               .replace("undefined", "\"undefined\"")
               .replace("\"\"undefined\"\"", "undefined") + "}";
            jsonObject = CrawlerUtils.stringToJson(split);
            break;
         }
      }
      return jsonObject;
   }

   @Override
   protected boolean hasNextPage() {
      return true;
   }
}
