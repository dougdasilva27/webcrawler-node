package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;

public class BrasilKitchenaidCrawler extends CrawlerRankingKeywords {

   public BrasilKitchenaidCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.APACHE;
   }

   private String keySHA256;
   private static final Integer API_VERSION = 1;
   private static final String SENDER = "kitchenaid2.store-components@0.x";
   private static final String PROVIDER = "kitchenaid2.store-graphql@0.x";

   @Override
   protected void extractProductsFromCurrentPage() {
      this.log("Página " + this.currentPage);
      this.pageSize = 42;

      if (this.currentPage == 1) {
         this.keySHA256 = fetchSHA256Key();
      }

      JSONObject searchApi = fetchSearchApi();
      JSONArray products = searchApi.has("products") ? searchApi.getJSONArray("products") : new JSONArray();

      if (products.length() > 0) {

         if (this.totalProducts == 0) {
            setTotalProducts(searchApi);
         }

         for (Object object : products) {
            JSONObject product = (JSONObject) object;
            String productUrl = CrawlerUtils.completeUrl(product.optString("url"), "https",
                  "www.kitchenaid.com.br");
            String internalPid = product.optString("productId");

            saveDataProduct(null, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);

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
      this.totalProducts = CrawlerUtils.getIntegerValueFromJSON(data, "size", 0);
      this.log("Total da busca: " + this.totalProducts);
   }

   /**
    * This function request a api with a JSON encoded on BASE64
    *
    * This json has informations like: pageSize, keyword and substantive {@link fetchSubstantive}
    *
    * @return
    */
   private JSONObject fetchSearchApi() {
      JSONObject searchApi = new JSONObject();

      StringBuilder url = new StringBuilder();
      url.append("https://www.kitchenaid.com.br/_v/public/graphql/v1?");
      url.append("workspace=master");
      url.append("&maxAge=short");
      url.append("&appsEtag=remove");
      url.append("&domain=store");
      url.append("&locale=pt-BR");
      url.append("&operationName=searchProducts");

      // https://www.kitchenaid.com.br/_v/public/graphql/v1?workspace=master&maxAge=short&appsEtag=remove&domain=store&locale=pt-BR&operationName=searchProducts&variables=%7B%7D&extensions=%7B%22persistedQuery%22%3A%7B%22version%22%3A1%2C%22sha256Hash%22%3A%229959d7b9df1407b7f1b2f589fe538180b9ffb83731ea6ec9fb0655e3d8ac3890%22%2C%22sender%22%3A%22kitchenaid2.store-components%400.x%22%2C%22provider%22%3A%22kitchenaid2.store-graphql%400.x%22%7D%2C%22variables%22%3A%22eyJ0ZXJtcyI6ImJhdGVkZWlyYSIsInBhZ2UiOjEsInNvcnRCeSI6InJlbGV2YW5jZSIsImZpbHRlciI6IiIsInJlc3VsdHNQZXJQYWdlIjo5fQ%3D%3D%22%7D

      JSONObject extensions = new JSONObject();
      JSONObject persistedQuery = new JSONObject();

      persistedQuery.put("version", API_VERSION);
      persistedQuery.put("sha256Hash", "9959d7b9df1407b7f1b2f589fe538180b9ffb83731ea6ec9fb0655e3d8ac3890");
      persistedQuery.put("sender", SENDER);
      persistedQuery.put("provider", PROVIDER);
      extensions.put("variables", createVariablesBase64());
      extensions.put("persistedQuery", persistedQuery);

      StringBuilder payload = new StringBuilder();
      try {
         payload.append("&variables=" + URLEncoder.encode("{}", "UTF-8"));
         payload.append("&extensions=" + URLEncoder.encode(extensions.toString(), "UTF-8"));
      } catch (UnsupportedEncodingException e) {
         Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
      }

      url.append(payload.toString());

      this.log("Link onde são feitos os crawlers: " + url);



      Request request = RequestBuilder.create()
            .setUrl(url.toString())
            .setCookies(cookies)
            .setPayload(payload.toString())
            .build();



      JSONObject response = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());


      if (response.has("data") && !response.isNull("data")) {
         JSONObject data = response.getJSONObject("data");

         if (data.has("searchProducts") && !data.isNull("searchProducts")) {
            searchApi = data.getJSONObject("searchProducts");
         }
      }

      return searchApi;
   }

   private String createVariablesBase64() {
      JSONObject search = new JSONObject();

      search.put("terms", this.keywordEncoded);
      search.put("page", this.currentPage);
      search.put("sortBy", "relevance");
      search.put("filter", "");
      search.put("resultsPerPage", 9);

      return Base64.getEncoder().encodeToString(search.toString().getBytes());
   }

   /**
    * This function accesses the search url and extracts a hash that will be required to access the
    * search api.
    *
    * This hash is inside a key in json STATE. Ex:
    *
    * "$ROOT_QUERY.productSearch({\"from\":0,\"hideUnavailableItems\":true,\"map\":\"ft\",\"orderBy\":\"OrderByTopSaleDESC\",\"query\":\"ACONDICIONADOR\",\"to\":19}) @runtimeMeta({\"hash\":\"0be25eb259af62c2a39f305122908321d46d3710243c4d4ec301bf158554fa71\"})"
    *
    * Hash: a627313bafddd8a80fa1ed6223a7bb55ea33e76959294b4f80b74bcd2ef86e8b
    *
    * @return
    */
   private String fetchSHA256Key() {
      // When sha256Hash is not found, this key below works (on 03/02/2020)
      String hash = "9959d7b9df1407b7f1b2f589fe538180b9ffb83731ea6ec9fb0655e3d8ac3890";
      // When script with hash is not found, we use this url
      String url = "https://www.kitchenaid.com.br/_v/public/assets/v1/published/bundle/public/react/asset.min.js?v=1&files=vtex.search@0.6.4,0";

      String homePage = "https://www.kitchenaid.com.br";

      Request requestHome = RequestBuilder.create().setUrl(homePage).setCookies(cookies).mustSendContentEncoding(false).build();
      Document doc = Jsoup.parse(this.dataFetcher.get(session, requestHome).getBody());

      Elements scripts = doc.select("body > script[crossorigin]");
      for (Element e : scripts) {
         String scriptUrl = CrawlerUtils.scrapUrl(e, null, "src", "https", "exitocol.vtexassets.com");
         if (scriptUrl.contains("vtex.search@")) {
            url = scriptUrl;
            break;
         }
      }


      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).mustSendContentEncoding(false).build();
      String response = this.dataFetcher.get(session, request).getBody().replace(" ", "");

      String searchProducts = CrawlerUtils.extractSpecificStringFromScript(response, "searchResult(", false, "',", false);
      String firstIndexString = "@runtimeMeta(hash:";
      if (searchProducts.contains(firstIndexString) && searchProducts.contains(")")) {
         int x = searchProducts.indexOf(firstIndexString) + firstIndexString.length();
         int y = searchProducts.indexOf(')', x);

         hash = searchProducts.substring(x, y).replace("\"", "");
      }

      return hash;
   }


}
