package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VTEXGraphQLRanking extends CrawlerRankingKeywords {
   public VTEXGraphQLRanking(Session session) {
      super(session);
      this.pageSize = session.getOptions().optInt("pageSize", 12);
   }

   private String HOME_PAGE = getHomePage();

   private String getHomePage() {
      return session.getOptions().optString("homePage");
   }

   @Override
   protected void processBeforeFetch() {
      JSONObject cookies = session.getOptions().optJSONObject("cookies");
      if (cookies != null) {
         for (String key : cookies.keySet()) {
            BasicClientCookie cookie = new BasicClientCookie(key, cookies.optString(key));
            cookie.setPath("/");
            cookie.setDomain(HOME_PAGE);
            this.cookies.add(cookie);
         }
      }
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      Document doc = fetchDocument(HOME_PAGE + this.keywordEncoded);
      JSONObject searchResult = fetchSearchApi(doc);

      if (searchResult != null && searchResult.has("products")) {
         if (this.totalProducts == 0) {
            this.totalProducts = searchResult.optInt("recordsFiltered");
         }

         for (Object object : searchResult.optJSONArray("products")) {
            JSONObject product = (JSONObject) object;

            String internalId = JSONUtils.getValueRecursive(product, "items.0.itemId", String.class, null);
            String internalPid = product.optString("productId");
            String url = HOME_PAGE + product.optString("linkText") + "/p";
            String name = product.optString("productName");
            String imgUrl = JSONUtils.getValueRecursive(product, "items.0.images.0.imageUrl", String.class, null);
            int price = scrapPrice(product);
            int stock = JSONUtils.getValueRecursive(product, "items.0.sellers.0.commertialOffer.AvailableQuantity", Integer.class, 0);
            boolean available = stock > 0;

            try {
               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(url)
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setImageUrl(imgUrl)
                  .setName(name)
                  .setPriceInCents(price)
                  .setAvailability(available)
                  .build();

               saveDataProduct(productRanking);
            } catch (MalformedProductException e) {
               this.log(e.getMessage());
            }
         }
      } else {
         log("keyword sem resultado");
      }
   }

   private int scrapPrice(JSONObject product) {
      Integer price = null;
      JSONObject sellingPrice = JSONUtils.getValueRecursive(product, "priceRange.sellingPrice", JSONObject.class, null);
      if (sellingPrice != null) {
         price = JSONUtils.getPriceInCents(sellingPrice, "lowPrice");
      }
      return price == 0 ? null : price;
   }

   private JSONObject fetchSearchApi(Document doc) {
      JSONObject searchApi = new JSONObject();
      StringBuilder url = new StringBuilder();
      String sha256Hash = getSha256Hash(doc);
      url.append(HOME_PAGE).append("_v/segment/graphql/v1?");

      JSONObject extensions = new JSONObject();
      JSONObject persistedQuery = new JSONObject();

      persistedQuery.put("version", "1");
      persistedQuery.put("sha256Hash", sha256Hash);
      persistedQuery.put("sender", "vtex.store-resources@0.x");
      persistedQuery.put("provider", "vtex.search-graphql@0.x");

      extensions.put("variables", createVariablesBase64());
      extensions.put("persistedQuery", persistedQuery);

      url.append("extensions=").append(URLEncoder.encode(extensions.toString(), StandardCharsets.UTF_8));
      this.log("Link onde são feitos os crawlers:" + url);

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
      JSONObject variables = session.getOptions().optJSONObject("variables");

      if (variables == null) {
         variables = new JSONObject();
         variables.put("hideUnavailableItems", false);
         variables.put("skusFilter", "ALL_AVAILABLE");
         variables.put("simulationBehavior", "default");
         variables.put("installmentCriteria", "MAX_WITHOUT_INTEREST");
         variables.put("productOriginVtex", false);
         variables.put("map", "ft");
         variables.put("orderBy", "");
         variables.put("facetsBehavior", "Static");
         variables.put("categoryTreeBehavior", "default");
         variables.put("withFacets", false);
      }

      variables.put("from", arrayProducts.size());
      variables.put("to", (arrayProducts.size() + this.pageSize) - 1);
      variables.put("query", this.keywordEncoded);

      JSONArray selectedFacets = new JSONArray();
      JSONObject obj = new JSONObject();
      obj.put("key", "ft");
      obj.put("value", this.keywordEncoded);

      selectedFacets.put(obj);

      variables.put("selectedFacets", selectedFacets);
      variables.put("fullText", this.keywordEncoded);

      return Base64.getEncoder().encodeToString(variables.toString().getBytes());
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
         sha256Hash = session.getOptions().optString("sha256Hash");
      }

      return sha256Hash;

   }
}