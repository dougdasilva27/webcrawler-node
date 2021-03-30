package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
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

      String url = HOME_PAGE + storeId + "/pick-up?ordenacao=relevance&conteudo=" + this.keywordEncoded + "&limite=24&offset=" + (this.currentPage - 1) * pageSize + "&ordenacao=relevance";
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
               ProxyCollection.INFATICA_RESIDENTIAL_BR_HAPROXY,
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
            ProxyCollection.INFATICA_RESIDENTIAL_BR,
            ProxyCollection.BUY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR));

         content = new FetcherDataFetcher().get(session, request).getBody();
      }

      return Jsoup.parse(content);

   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException {
      this.pageSize = 24;
      this.log("Página " + this.currentPage);

      Document doc = fetchPage();

      JSONObject json = selectJsonFromHtml(doc);

      JSONArray products = JSONUtils.getValueRecursive(json, "productGrid.items", JSONArray.class);

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts(json);
         }
         for (Object e : products) {
            if (e instanceof JSONObject) {
               JSONObject id = (JSONObject) e;

               String internalId = id.optString("id");
               String productUrl = HOME_PAGE + storeId + "/ship?ordenacao=relevance&conteudo=" + internalId;

               saveDataProduct(internalId, null, productUrl);

               this.log(
                  "Position: " + this.position +
                     " - InternalId: " + internalId +
                     " - InternalPid: " + null +
                     " - Url: " + productUrl);

               if (this.arrayProducts.size() == productsLimit) {
                  break;
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
            String decode = URLDecoder.decode(script, "UTF-8");
            String split = CrawlerUtils.extractSpecificStringFromScript(decode, "window.__PRELOADED_STATE__ = \"", true, "\";", true);
            jsonObject = CrawlerUtils.stringToJson(split);
            break;
         }
      }


      return jsonObject;
   }


   private void setTotalProducts(JSONObject json) {
      this.totalProducts = JSONUtils.getValueRecursive(json, "pagination.total", Integer.class);
      this.log("Total da busca: " + this.totalProducts);
   }
}
