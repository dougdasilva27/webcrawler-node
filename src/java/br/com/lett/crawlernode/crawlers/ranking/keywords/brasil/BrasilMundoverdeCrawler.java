package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.VTEXRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Base64;

public class BrasilMundoverdeCrawler extends VTEXRankingKeywords {

   private static final Integer API_VERSION = 1;
   private static final String SENDER = "vtex.store-resources@0.x";
   private static final String PROVIDER = "vtex.search-graphql@0.x";
   private String keySHA256 = "4136d62c555a4b2e1ba9a484a16390d6a6035f51760d5726f436380fa290d0cc";

   public BrasilMundoverdeCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return session.getOptions().getString("homePage");
   }

   @Override
   protected String getLocation() {
      return "";
   }

   @Override
   protected String getVtexSegment() {
      return session.getOptions().getString("vtex_segment");
   }

   @Override
   protected String createVariablesBase64() {
      JSONObject search = new JSONObject();
      search.put("hideUnavailableItems", false);
      search.put("skusFilter", "ALL");
      search.put("simulationBehavior", "default");
      search.put("installmentCriteria", "MAX_WITHOUT_INTEREST");
      search.put("productOriginVtex", true);
      search.put("map", "ft");
      search.put("query", keywordEncoded);
      search.put("orderBy", "");
      search.put("from", this.arrayProducts.size());
      search.put("to", this.arrayProducts.size() + (this.pageSize - 1));

      JSONArray selectedFacets = new JSONArray();
      JSONObject obj = new JSONObject();
      obj.put("key", "ft");
      obj.put("value", this.keywordEncoded);

      selectedFacets.put(obj);

      search.put("selectedFacets", selectedFacets);
      search.put("fullText", this.location);
      search.put("operator", JSONObject.NULL);
      search.put("fuzzy", JSONObject.NULL);
      search.put("facetsBehavior", "Static");
      search.put("categoryTreeBehavior", "default");
      search.put("withFacets", false);

      return Base64.getEncoder().encodeToString(search.toString().getBytes());
   }

   @Override
   protected JSONObject fetchSearchApi() {
      JSONObject searchApi;
      StringBuilder url = new StringBuilder();
      url.append(getHomePage() + "_v/segment/graphql/v1?");

      JSONObject extensions = new JSONObject();
      JSONObject persistedQuery = new JSONObject();

      persistedQuery.put("version", API_VERSION);
      persistedQuery.put("sha256Hash", this.keySHA256);
      persistedQuery.put("sender", SENDER);
      persistedQuery.put("provider", PROVIDER);

      extensions.put("variables", createVariablesBase64());
      extensions.put("persistedQuery", persistedQuery);

      StringBuilder payload = new StringBuilder();
      payload.append("workspace=master");
      payload.append("&maxAge=short");
      payload.append("&appsEtag=remove");
      payload.append("&domain=store");
      payload.append("&locale=pt-BR");
      payload.append("&operationName=productSearchV3");
      try {
         payload.append("&variables=" + URLEncoder.encode("{}", "UTF-8"));
         payload.append("&extensions=" + URLEncoder.encode(extensions.toString(), "UTF-8"));
      } catch (UnsupportedEncodingException e) {
         Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
      }
      url.append(payload.toString());

      log("Link onde são feitos os crawlers:" + url);

      Request request = Request.RequestBuilder.create()
         .setUrl(url.toString())
         .setCookies(cookies)
         .setPayload(payload.toString())
         .build();

      JSONObject response = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

      searchApi = JSONUtils.getValueRecursive(response, "data.productSearch", JSONObject.class, new JSONObject());

      return searchApi;
   }
//   public BrasilMundoverdeCrawler(Session session) {
//      super(session);
//   }
//
//   @Override
//   protected void extractProductsFromCurrentPage() {
//
//      this.pageSize = 12;
//
//      this.log("Página " + this.currentPage);
//
//      String url = "https://www.mundoverde.com.br/" + this.keywordEncoded + "?map=ft&page=" + this.currentPage;
//      this.log("Link onde são feitos os crawlers: " + url);
//
//      this.currentDoc = fetchDocument(url);
//
//      Elements products = this.currentDoc.select("div.vtex-search-result-3-x-galleryItem");
//
//      if (!products.isEmpty()) {
//         if (this.totalProducts == 0) {
//            setTotalProducts();
//         }
//
//         if (products.size() >= 1) {
//            for (Element e : products) {
//
//               String urlProduct = CrawlerUtils.scrapUrl(e, "a", "href", "https://", "www.mundoverde.com.br");
//
//               saveDataProduct(null, null, urlProduct);
//
//               this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + null + " - Url: " + urlProduct);
//               if (this.arrayProducts.size() == productsLimit) break;
//
//            }
//         } else {
//            this.result = false;
//            this.log("Keyword sem resultado!");
//         }
//
//         this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
//
//      }
//   }
//
//   @Override
//   protected void setTotalProducts() {
//      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(currentDoc, ".vtex-search-result-3-x-totalProducts--layout span", null, null, false, false, 0);
//      this.log("Total de produtos: " + this.totalProducts);
//   }
}


