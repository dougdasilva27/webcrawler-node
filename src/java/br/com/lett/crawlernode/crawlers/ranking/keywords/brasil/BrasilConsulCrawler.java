package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.JSONUtils;
import org.eclipse.jetty.util.UrlEncoded;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Base64;

public class BrasilConsulCrawler extends CrawlerRankingKeywords {

   public BrasilConsulCrawler(Session session) {
      super(session);
   }

   private static String  keySHA256 = "e6fe88ec9fbff68032ddf6abff44828ebba66558698ac37cf23f743ca17671b8";

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException{
      this.pageSize = 9;

      this.log("Página " + this.currentPage);

      String key = this.keywordWithoutAccents.replaceAll(" ", "%20");

      String url = buildSearchUrl(key, this.currentPage);
      this.log("Link onde são feitos os crawlers: " + url);

      JSONObject json = fetchJSONObject(url);

       JSONArray products = JSONUtils.getValueRecursive(json, "data.getSearchResultsProducts.items", JSONArray.class);

      if (products != null && !products.isEmpty()) {
         if (this.totalProducts == 0) {
            this.totalProducts = JSONUtils.getValueRecursive(json, "data.getSearchResultsProducts.size", Integer.class);
         }

         for (Object o : products) {
            JSONObject product = (JSONObject)o;

            String internalPid = product.optString("productId");

            String linkText = product.optString("linkText");
            String productUrl = "https://loja.consul.com.br/" + linkText + "/p";

            saveDataProduct(null, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);
            if (this.arrayProducts.size() == productsLimit) break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {
      return this.arrayProducts.size() < this.totalProducts;
   }

   private String buildSearchUrl(String keyword, int page) throws UnsupportedEncodingException {
      StringBuilder url = new StringBuilder();
      url.append("https://loja.consul.com.br/_v/segment/graphql/v1?workspace=master&maxAge=short&appsEtag=remove" +
         "&domain=store" +
         "&locale=pt-BR" +
         "&operationName=QueryGetSearchResultsProducts" +
         "&variables=%7B%7D" +
         "&extensions=");

      JSONObject variables = new JSONObject();
      variables.put("terms", keyword);
      variables.put("page", page);
      variables.put("source", "desktop");
      variables.put("pids", "");
      variables.put("resultsPerPage", 9);
      variables.put("sortBy", "relevance");
      variables.put("filter", "");
      variables.put("hide", "");
      variables.put("showOnlyAvailable", true);
      variables.put("allowRedirect", true);

      String variablesBase64 = Base64.getEncoder().encodeToString(variables.toString().getBytes());

      JSONObject persistedQuery = new JSONObject();
      persistedQuery.put("version", 1);
      persistedQuery.put("sha256Hash", this.keySHA256);
      persistedQuery.put("sender", "consul.chaordic@0.x");
      persistedQuery.put("provider", "consul.chaordic@0.x");

      JSONObject persistedQueryJson = new JSONObject();
      persistedQueryJson.put("persistedQuery", persistedQuery);
      persistedQueryJson.put("variables", variablesBase64);

      String queryUrlEncoded = URLEncoder.encode(persistedQueryJson.toString(), "UTF-8");
      url.append(queryUrlEncoded);

      return url.toString();
   }
}
