package br.com.lett.crawlernode.crawlers.ranking.keywords.guatemala;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GuatemalaWalmartCrawler extends CrawlerRankingKeywords {

   private static String HOME_PAGE = "https://www.walmart.com.gt/";

   public GuatemalaWalmartCrawler(Session session) {
      super(session);
   }


   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException {
      Document doc = fetchDocument(HOME_PAGE + this.keywordEncoded);
      JSONObject searchResult = fetchSearchApi(doc);

      if (searchResult != null && searchResult.has("products")) {

         if (this.totalProducts == 0) {
            this.totalProducts = searchResult.optInt("recordsFiltered");
         }

         for (Object object : searchResult.optJSONArray("products")) {
            JSONObject products = (JSONObject) object;

            String internalId = JSONUtils.getValueRecursive(products, "items.0.itemId", String.class);
            String internalPid = products.optString("productId");
            String url = HOME_PAGE + products.optString("linkText") + "/p";

            saveDataProduct(internalId, internalPid, url);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + url);
         }


      } else {
         log("keyword sem resultado");
      }
   }


   private JSONObject fetchSearchApi(Document doc) {
      JSONObject searchApi = new JSONObject();
      StringBuilder url = new StringBuilder();
      String sha256Hash = getSha256Hash(doc);
      url.append(HOME_PAGE + "_v/segment/graphql/v1?");

      JSONObject extensions = new JSONObject();
      JSONObject persistedQuery = new JSONObject();

      persistedQuery.put("version", "1");
      persistedQuery.put("sha256Hash", sha256Hash);
      persistedQuery.put("sender", "vtex.store-resources@0.x");
      persistedQuery.put("provider", "vtex.search-graphql@0.x");

      extensions.put("variables", createVariablesBase64());
      extensions.put("persistedQuery", persistedQuery);

      StringBuilder payload = new StringBuilder();

      try {
         payload.append("extensions=" + URLEncoder.encode(extensions.toString(), "UTF-8"));
      } catch (UnsupportedEncodingException e) {
         Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
      }


      url.append(payload.toString());
      log("Link onde são feitos os crawlers:" + url);

      Request request = Request.RequestBuilder.create()
         .setUrl(url.toString())
         .setCookies(cookies)
         .build();

      JSONObject response = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

      if (response.has("data") && !response.isNull("data")) {
         JSONObject data = response.getJSONObject("data");

         if (data.has("productSearch") && !data.isNull("productSearch")) {
            searchApi = data.getJSONObject("productSearch");

         }
      }

      return searchApi;
   }

   private String createVariablesBase64() {
      JSONObject search = new JSONObject();
      search.put("hideUnavailableItems", false);
      search.put("skusFilter", "ALL");
      search.put("simulationBehavior", "skip");
      search.put("installmentCriteria", "MAX_WITHOUT_INTEREST");
      search.put("productOriginVtex", true);
      search.put("map", "ft");
      search.put("query", this.keywordEncoded);
      search.put("orderBy", "");
      search.put("from", 0);
      search.put("to", 20);

      JSONArray selectedFacets = new JSONArray();
      JSONObject obj = new JSONObject();
      obj.put("key", "ft");
      obj.put("value", this.keywordEncoded);

      selectedFacets.put(obj);

      search.put("selectedFacets", selectedFacets);
      search.put("fullText", this.keywordEncoded);
      search.put("facetsBehavior", "Static");
      search.put("categoryTreeBehavior", "default");
      search.put("withFacets", false);

      return Base64.getEncoder().encodeToString(search.toString().getBytes());
   }

   private String getScript(Element element) {
      String script = null;
      Pattern pattern = Pattern.compile("\\<script>(.*)<\\/script>");
      Matcher matcher = pattern.matcher(element.toString());
      if (matcher.find()) {
         script = matcher.group(1);
      }
      return script;
   }


   private String getSha256Hash(Document doc) {
      Element el = doc.selectFirst("template[data-varname='__STATE__']");
      String sha256Hash = null;

      String script = getScript(el);
      if (script != null && !script.isEmpty()) {
         JSONObject jsonObject = CrawlerUtils.stringToJson(script);

         for (String key : jsonObject.keySet()) {
            String firstIndexString = "@runtimeMeta(";
            String keyIdentifier = "$ROOT_QUERY.productSearch";

            if (key.contains(firstIndexString) && key.contains(keyIdentifier) && key.endsWith(")")) {
               int x = key.indexOf(firstIndexString) + firstIndexString.length();
               int y = key.indexOf(')', x);

               JSONObject hashJson = CrawlerUtils.stringToJson(key.substring(x, y).replace("\\\"", "\""));

               if (hashJson.has("hash") && !hashJson.isNull("hash")) {
                  sha256Hash = hashJson.get("hash").toString();
               }

               break;
            }

         }
      }

      if (sha256Hash == null) {
         sha256Hash = "4136d62c555a4b2e1ba9a484a16390d6a6035f51760d5726f436380fa290d0cc";
      }

      return sha256Hash;

   }

}
