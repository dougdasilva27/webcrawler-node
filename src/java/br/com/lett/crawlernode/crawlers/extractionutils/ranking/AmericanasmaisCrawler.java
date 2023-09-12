package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.HttpClientFetcher;
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
import com.google.common.net.HttpHeaders;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.*;

public class AmericanasmaisCrawler extends CrawlerRankingKeywords {

   public AmericanasmaisCrawler(Session session) {
      super(session);
   }

   private final String storeId = getStoreId();

   protected Map<String, String> headers = getHeaders();
   private static final String HOME_PAGE = "https://www.americanas.com.br/lojas-proximas/33014556000196/";

   public String getStoreId() {
      return session.getOptions().optString("store_id");
   }

   private static final List<String> UserAgent = Arrays.asList(
      "Mozilla/5.0 (iPhone; CPU iPhone OS 14_7 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/93.0.4577.39 Mobile/15E148 Safari/604.1",
      "Mozilla/5.0 (iPad; CPU OS 14_7 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/93.0.4577.39 Mobile/15E148 Safari/604.1",
      "Mozilla/5.0 (iPod; CPU iPhone OS 14_7 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/93.0.4577.39 Mobile/15E148 Safari/604.1",
      "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.62 Mobile Safari/537.36",
      "Mozilla/5.0 (Linux; Android 10; SM-A205U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.62 Mobile Safari/537.36",
      "Mozilla/5.0 (Linux; Android 10; SM-A102U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.62 Mobile Safari/537.36",
      "Mozilla/5.0 (Linux; Android 10; SM-G960U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.62 Mobile Safari/537.36",
      "Mozilla/5.0 (Linux; Android 10; LM-X420) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.62 Mobile Safari/537.36",
      "Mozilla/5.0 (Linux; Android 10; LM-Q710(FGN)) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.62 Mobile Safari/537.36",
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36"
   );

   public static Map<String, String> getHeaders() {
      Random random = new Random();

      Map<String, String> headers = new HashMap<>();

      headers.put("user-agent", UserAgent.get(random.nextInt(UserAgent.size())));
      headers.put(HttpHeaders.REFERER, HOME_PAGE);
      headers.put(
         HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7"
      );
      headers.put(HttpHeaders.CACHE_CONTROL, "max-age=0");
      headers.put("authority", "www.americanas.com.br");
      headers.put(HttpHeaders.ACCEPT_LANGUAGE, "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");
      headers.put("sec-fetch-site", "none");
      headers.put("sec-fetch-mode", "navigate");
      headers.put("sec-fetch-user", "?1");
      headers.put("sec-fetch-dest", "document");

      return headers;
   }

   private Document fetchPage() {

      String url = HOME_PAGE + storeId + "?ordenacao=topSelling&conteudo=" +
         this.keywordEncoded.replace("+", "%20") +
         "&limite=24&offset=" + (this.currentPage - 1) * pageSize;

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .mustSendContentEncoding(false)
         .setHeaders(headers)
         .setFetcheroptions(
            FetcherOptions.FetcherOptionsBuilder.create()
               .mustUseMovingAverage(false)
               .mustRetrieveStatistics(true)
               .build()
         ).setProxyservice(
            Arrays.asList(
               ProxyCollection.NETNUT_RESIDENTIAL_ROTATE_BR
            )
         ).build();

      Response response = CrawlerUtils.retryRequest(request, session, new ApacheDataFetcher(), true);
      String content = response.getBody();

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

      if (products != null && !products.isEmpty()) {
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
