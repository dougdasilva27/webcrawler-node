package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Map;

public class SavegnagoRanking extends CrawlerRankingKeywords {

   private static final String BASE_URL = "www.savegnago.com.br";
   private String urlModel;
   private final String storeId = getStoreId();


   public String getStoreId() {
      return storeId;
   }

   public SavegnagoRanking(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   @Override
   protected void processBeforeFetch() {
      super.processBeforeFetch();

      Logging.printLogDebug(logger, session, "Adding cookie...");

      BasicClientCookie cookie = new BasicClientCookie("VTEXSC", "sc=" + storeId);
      cookie.setDomain(".savegnago.com.br");
      cookie.setPath("/");
      this.cookies.add(cookie);

   }


   private String buildUrl() {
      String host = "http://";
      if (urlModel == null) {
         String urlFirst = "https://" + BASE_URL + "/" + this.keywordEncoded;

         this.currentDoc = getHtml(urlFirst);
         Element element = this.currentDoc.selectFirst(".vitrine script[type='text/javascript']");

         if (element != null) {
            String script = element.toString();
            String[] firstSplit = script.split("load\\('");
            if (firstSplit.length > 0) {
               String url = firstSplit[1].split("' \\+ pageclickednumber")[0];
               urlModel = BASE_URL + url;
               return host + urlModel + this.currentPage;
            }
         } else {
            return null;
         }
      } else {
         return host + urlModel + this.currentPage;
      }
      return null;
   }

   private Document getHtml(String url) {
      Request request = Request.RequestBuilder.create().setUrl(url).setCookies(cookies).build();
      Response response = dataFetcher.get(session, request);

      return Jsoup.parse(response.getBody());

   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 16;
      this.log("Página " + this.currentPage);

      String url = buildUrl();

      if (url != null) {
         extract(url);
      } else {
         JSONObject json = loadJson();
         if (json.has("products")) {
            extractJson(json);
         } else {
            result = false;
            log("Keyword sem resultado!");
         }

      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");

   }

   private void extract(String url) {
      this.currentDoc = fetchDocument(url);

      if (currentDoc.selectFirst(".product-card") != null) {
         //Get from the html
         this.log("Link onde são feitos os crawlers: " + url);
         Elements products = this.currentDoc.select(".n4colunas li[layout]");

         if (products != null && !products.isEmpty()) {
            if (totalProducts == 0) {
               setTotalProducts();
            }
            for (Element product : products) {
               String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".product-card", "item");
               String internalPid = internalId;
               String productUrl = CrawlerUtils.scrapUrl(product, ".prod-acc > a", "href", "https", BASE_URL);
               saveDataProduct(internalId, internalPid, productUrl);

               log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
               if (arrayProducts.size() == productsLimit) {
                  break;
               }
            }
         }
      }
   }

   private void extractJson(JSONObject json) {

      JSONArray products = json.optJSONArray("products");

      if (products != null && !products.isEmpty()) {
         if (totalProducts == 0) {
            setTotalProducts(json);
         }
         for (Object obj : products) {
            if (obj instanceof JSONObject) {
               JSONObject product = (JSONObject) obj;
               String internalId = product.optString("id");
               String internalPid = internalId;
               String url = product.optString("url");
               String productUrl = url != null ? "http:" + url : null;
               saveDataProduct(internalId, internalPid, productUrl);

               log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
               if (arrayProducts.size() == productsLimit) {
                  break;
               }
            }
         }
      }
   }

   private JSONObject loadJson() {
      String url = "https://api.linximpulse.com/engage/search/v3/search/?salesChannel=" + storeId + "&apiKey=savegnago&terms=" + this.keywordEncoded + "&resultsPerPage=32&page=" + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);
      Map<String, String> headers = new HashMap<>();
      headers.put("origin", "https://www.savegnago.com.br");

      Request request = Request.RequestBuilder.create().setUrl(url)
         .setHeaders(headers)
         .build();
      Response response = this.dataFetcher.get(session, request);
      return CrawlerUtils.stringToJson(response.getBody());

   }

   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".resultado-busca-numero .value", true, 0);
      this.log("Total de produtos: " + this.totalProducts);
   }

   protected void setTotalProducts(JSONObject json) {
      this.totalProducts = json.optInt("size");
      this.log("Total de produtos: " + this.totalProducts);
   }

}
