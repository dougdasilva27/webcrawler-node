package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
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
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.B2WScriptPageCrawlerRanking;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SaopauloAmericanasCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "https://www.americanas.com.br/";

   public SaopauloAmericanasCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.JSOUP;
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 24;
      this.log("Página " + this.currentPage);

      this.currentDoc = fetchPage();
      JSONObject json = CrawlerUtils.selectJsonFromHtml(this.currentDoc, "body > script", "window.__APOLLO_STATE__ =", null, false, false);
      JSONArray products = CrawlerUtils.stringToJsonArray(getStringWithRegex(json, "products\":([\\s\\S]*?)},\"device\""));

      if (products != null && !products.isEmpty()) {
         if (this.totalProducts == 0){
            setTotalProducts(json);
         }
         for (Object e : products) {
            if (e instanceof JSONObject) {
               JSONObject productJson = (JSONObject) e;
               JSONObject productInfo = productJson.optJSONObject("product");

               if (productInfo != null && !productInfo.isEmpty()) {
                  JSONObject offers = getJson(productInfo);
                  String internalId = JSONUtils.getValueRecursive(offers, "result.0.sku", String.class);
                  String internalPid = productInfo.optString("id");
                  String productUrl = HOME_PAGE + "produto/" + internalPid;
                  String name = productInfo.optString("name");
                  JSONArray imageJson = getJsonArray(productInfo);
                  String imageUrl = JSONUtils.getValueRecursive(imageJson, "0.large", String.class);
                  int price = CommonMethods.doublePriceToIntegerPrice(JSONUtils.getValueRecursive(offers, "result.0.salesPrice", Double.class), 0);
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
         if (script.contains("window.__APOLLO_STATE__ =")) {
            String readyToDecode = script.replace("%", "%25");
            String decode = URLDecoder.decode(readyToDecode, "UTF-8");
            String split = CrawlerUtils.getStringBetween(decode, "window.__APOLLO_STATE__ =", ",\"session\":") + "}";
            jsonObject = CrawlerUtils.stringToJson(split);
            break;
         }
      }

      return jsonObject;
   }

   private String getStringWithRegex(JSONObject jsonObject, String regex) {
      String url = null;
      Pattern pattern = Pattern.compile(regex);
      Matcher matcher = pattern.matcher(jsonObject.toString());
      if (matcher.find()) {
         url = matcher.group(1);
      }
      return url;
   }

   private JSONObject getJson(JSONObject jsonObject) {
      for (Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
         String key = it.next();
         if (key.contains("offers")) {
            return jsonObject.optJSONObject(key);
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
      String total = getStringWithRegex(json, "\"total\":(.*?),");
      this.totalProducts = Integer.parseInt(total);
      this.log("Total da busca: " + this.totalProducts);
   }

   protected Document fetchPage() {
      String keyword = this.keywordWithoutAccents.replace(" ", "-");

      String url = HOME_PAGE + "busca/" + keyword + "?chave_search=achistory&limit=24&offset=" + (this.currentPage - 1) * pageSize;
      this.log("Link onde são feitos os crawlers: " + url);

      Map<String,String> headers = new HashMap<>();

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

      return Jsoup.parse(br.com.lett.crawlernode.crawlers.corecontent.saopaulo.SaopauloAmericanasCrawler.fetchPage(url,this.dataFetcher,this.cookies,headers,session));
   }



}
