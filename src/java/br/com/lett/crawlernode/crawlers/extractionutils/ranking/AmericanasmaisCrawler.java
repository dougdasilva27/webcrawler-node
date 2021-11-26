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
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public abstract class AmericanasmaisCrawler extends CrawlerRankingKeywords {

   protected AmericanasmaisCrawler(Session session) {
      super(session);
   }

   private final String storeId = getStoreId();

   private static final String HOME_PAGE = "https://www.americanas.com.br/lojas-proximas/33014556000196/";

   public String getStoreId() {
      return storeId;
   }

   private Document fetchPage() {

      Map<String, String> headers = new HashMap<>();

      headers.put("authority", "www.americanas.com.br");
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

      JSONObject json = selectJsonFromHtml(doc);

      JSONObject products = json.optJSONObject("products");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts(json);
         }
         for (String internalId : products.toMap().keySet()) {
            String productUrl = HOME_PAGE + storeId + "/ship?ordenacao=relevance&conteudo=" + internalId;
            JSONObject productInfo = products.optJSONObject(internalId);
            if (productInfo != null) {
               String name = productInfo.optString("name");
               String imgUrl = JSONUtils.getValueRecursive(productInfo, "images.0.large", String.class);
               Integer price = CommonMethods.doublePriceToIntegerPrice(JSONUtils.getValueRecursive(productInfo, "offers.result.0.salesPrice", Double.class), 0);
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

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");

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

   private void setTotalProducts(JSONObject json) {
      JSONObject jsonObject = json.optJSONObject("pages");
      for (Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
         String key = it.next();
         if (!key.equals("type")) {
            JSONObject jsonObjectWithKey = jsonObject.optJSONObject(key);
            this.totalProducts = JSONUtils.getValueRecursive(jsonObjectWithKey, "queries.getStoreOffersAcom.result.search.total", Integer.class);
            ;
            this.log("Total da busca: " + this.totalProducts);
            break;
         }
      }

   }
}
