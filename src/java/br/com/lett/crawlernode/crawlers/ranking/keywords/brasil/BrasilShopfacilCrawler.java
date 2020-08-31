package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Base64;

public class BrasilShopfacilCrawler extends CrawlerRankingKeywords {

   public BrasilShopfacilCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   private static final String API_VERSION = "omnilogic.search@0.5.0";

   private String hash;

   @Override
   protected void extractProductsFromCurrentPage() {
      this.log("Página " + this.currentPage);
      this.pageSize = 24;

      JSONObject searchApi = fetchSearchApi();
      JSONArray products = searchApi.has("results") ? searchApi.getJSONArray("results") : new JSONArray();

      if (products.length() > 0) {

         if (this.totalProducts == 0) {
            setTotalProducts(searchApi);
         }

         for (Object object : products) {
            JSONObject product = (JSONObject) object;
            String productUrl = product.has("url") ? CrawlerUtils.completeUrl(product.get("url").toString(), "https", "www.shopfacil.com.br") : null;
            String internalId = product.has("sku") ? product.get("sku").toString() : null;

            saveDataProduct(internalId, null, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);

            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }

      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private void setTotalProducts(JSONObject data) {
      if (data.has("total") && data.get("total") instanceof Integer) {
         this.totalProducts = data.getInt("total");
         this.log("Total da busca: " + this.totalProducts);
      }
   }

   @Override
   protected boolean hasNextPage() {
      return false;
   }

   /**
    * This function request a api with a JSON encoded on BASE64
    * <p>
    * This json has informations like: pageSize, keyword and substantive {@link fetchSubstantive}
    *
    * @return
    */
   private JSONObject fetchSearchApi() {
      JSONObject searchApi = new JSONObject();

      StringBuilder url = new StringBuilder();
      url.append("https://www.shopfacil.com.br/api/io/_v/public/graphql/v1?")
         .append("workspace=master")
         .append("&maxAge=short")
         .append("&appsEtag=c965db37630feb1167bda84b8ea48c42b2114d7a")
         .append("&domain=store")
         .append("&locale=pt-BR")
         .append("&operationName=ListOffers");


      JSONObject extensions = new JSONObject();
      JSONObject persistedQuery = new JSONObject();

      persistedQuery.put("version", API_VERSION);
      persistedQuery.put("sha256Hash", fetchSHA256Key());
      extensions.put("persistedQuery", persistedQuery);
      extensions.put("variables", createVariablesBase64());

      StringBuilder payload = new StringBuilder();

      try {
         payload.append("&variables=").append(URLEncoder.encode("{}", "UTF-8"));
         payload.append("&extensions=").append(URLEncoder.encode(extensions.toString(), "UTF-8"));
      } catch (UnsupportedEncodingException e) {
         Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
      }

      url.append(payload.toString());
      this.log("Link onde são feitos os crawlers: " + url);

      System.out.println(url.toString());

      Request request = RequestBuilder.create().setUrl(url.toString()).setCookies(cookies).setPayload(payload.toString()).mustSendContentEncoding(false).build();
      JSONObject response = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

      if (response.has("data")) {
         JSONObject data = response.getJSONObject("data");

         if (data.has("search")) {
            searchApi = data.getJSONObject("search");
         }
      }

      return searchApi;
   }

   private String createVariablesBase64() {
      JSONObject search = new JSONObject();
      search.put("pageSize", this.productsLimit);
      search.put("sort", "score.desc");
      search.put("searchPath", "/busca/");
      search.put("selectedSubstantive", "none");
      search.put("source", "page");
      search.put("categories", JSONObject.NULL);
      search.put("priceRange", JSONObject.NULL);
      search.put("priceDiscount", JSONObject.NULL);
      search.put("clusters", JSONObject.NULL);
      search.put("sellers", JSONObject.NULL);
      search.put("ignoreSuggestions", true);

      JSONArray metadata = new JSONArray();
      JSONObject keywordJson = new JSONObject();
      keywordJson.put("name", "termos");
      keywordJson.put("values", new JSONArray().put(this.keywordWithoutAccents));
      metadata.put(keywordJson);

      search.put("metadata", metadata);

      return Base64.getEncoder().encodeToString(search.toString().getBytes());
   }

   /**
    * This function accesses the search url and extracts a hash that will be required to access the
    * search api.
    * <p>
    * This hash is inside a key in json STATE. Ex:
    * <p>
    * "$ROOT_QUERY.productSearch({\"from\":0,\"hideUnavailableItems\":true,\"map\":\"ft\",\"orderBy\":\"OrderByTopSaleDESC\",\"query\":\"ACONDICIONADOR\",\"to\":19}) @runtimeMeta({\"hash\":\"0be25eb259af62c2a39f305122908321d46d3710243c4d4ec301bf158554fa71\"})"
    * <p>
    * Hash: 1d1ad37219ceb86fc281aa774971bbe1fe7656730e0a2ac50ba63ed63e45a2a3
    *
    * @return String hash
    */
   private String fetchSHA256Key() {

      if (hash != null) {
         return hash;
      }
      String url = "https://www.shopfacil.com.br/busca/?io_text=termos--" + this.keywordWithoutAccents.replace(" ", "-");

      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
      String response = this.dataFetcher.get(session, request).getBody();

      if (response != null) {
         Document doc = Jsoup.parse(response);
         JSONObject stateJson = CrawlerUtils.selectJsonFromHtml(doc, "script", "__STATE__=", ";", true, true);

         for (String key : stateJson.keySet()) {
            String firstIndexString = "@runtimeMeta(";
            String keyIdentifier = "$ROOT_QUERY.search";

            if (key.contains(firstIndexString) && key.contains(keyIdentifier) && key.endsWith(")")) {
               int x = key.indexOf(firstIndexString) + firstIndexString.length();
               int y = key.indexOf(')', x);

               JSONObject hashJson = CrawlerUtils.stringToJson(key.substring(x, y).replace("\\\"", "\""));

               if (hashJson.has("hash") && !hashJson.isNull("hash")) {
                  hash = hashJson.get("hash").toString();
               }

               break;
            }
         }
      }

      if (hash == null) {
         hash = "2da1e09e3e9fb5f2e1ad240b91afa44914933ec59f8ab99cc52d0be296923299";
      }
      return hash;
   }
}
