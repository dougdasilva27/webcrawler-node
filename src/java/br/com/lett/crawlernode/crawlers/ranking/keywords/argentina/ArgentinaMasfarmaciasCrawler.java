package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class ArgentinaMasfarmaciasCrawler extends CrawlerRankingKeywords {

   private static HashMap<String, String> searchParameters;

   public ArgentinaMasfarmaciasCrawler(Session session) {
      super(session);
   }

   private Map<String, String> getHeaders(){
      Map<String, String> headers = new HashMap<>();
      headers.put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36");
      headers.put("X-Requested-With", "XMLHttpRequest");
      return headers;
   }

   public Document fetchPage(String url) {

      Map<String, String> headers = getHeaders();

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build();

      return Jsoup.parse(this.dataFetcher
         .get(session, request)
         .getBody());
   }

   public Document fetchNextPage(String url, String payload) {

      Map<String, String> headers = getHeaders();
      headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload)
         .build();

      return Jsoup.parse(new JsoupDataFetcher()
         .post(session, request)
         .getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 16;
      this.log("Página " + this.currentPage);

      if (this.currentPage == 1) {
         String url = "https://www.masfarmacias.com/?s=" + this.keywordEncoded + "&post_type=product&type_aws=true";
         this.currentDoc = fetchPage(url);

         searchParameters = crawlSearchParameters(this.currentDoc);

         this.log("Link onde são feitos os crawlers: " + url);
      }else{
         String url = "https://www.masfarmacias.com/wp-admin/admin-ajax.php";

         String payloadContent = createPayload(this.keywordEncoded,
            this.currentPage,
            searchParameters.get("maxNumPages"),
            searchParameters.get("widgetId"),
            searchParameters.get("postId"),
            searchParameters.get("themeId"));

         this.currentDoc = fetchNextPage(url, payloadContent);
      }

      Elements products = this.currentDoc.select("article.elementor-grid-item");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            this.totalProducts = products.size();
            this.log("Total da busca: " + this.totalProducts);
         }
         for (Element e : products) {
            String internalId = e.attr("id").replace("post-", "");
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".elementor-image > a", "href");

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
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");

   }

   @Override
   protected boolean hasNextPage() {
      return this.currentPage <= Integer.parseInt(searchParameters.get("maxNumPages"));
   }

   private String createPayload(String keyword, int currentPage, String maxNumPages, String widgetId, String postId, String themeId) {
      StringBuilder payload = new StringBuilder();

      try {
      payload.append("action=ecsload&");
      payload.append("query=");
      payload.append(URLEncoder.encode("{\"s\":\"" + keyword + "\",\"post_type\":\"product\",\"error\":\"\",\"m\":\"\",\"p\":0,\"post_parent\":\"\",\"subpost\":\"\",\"subpost_id\":\"\",\"attachment\":\"\",\"attachment_id\":0,\"name\":\"\",\"pagename\":\"\",\"page_id\":0,\"second\":\"\",\"minute\":\"\",\"hour\":\"\",\"day\":0,\"monthnum\":0,\"year\":0,\"w\":0,\"category_name\":\"\",\"tag\":\"\",\"cat\":\"\",\"tag_id\":\"\",\"author\":\"\",\"author_name\":\"\",\"feed\":\"\",\"tb\":\"\",\"paged\":0,\"meta_key\":\"\",\"meta_value\":\"\",\"preview\":\"\",\"sentence\":\"\",\"title\":\"\",\"fields\":\"\",\"menu_order\":\"\",\"embed\":\"\",\"category__in\":[],\"category__not_in\":[],\"category__and\":[],\"post__in\":[],\"post__not_in\":[],\"post_name__in\":[],\"tag__in\":[],\"tag__not_in\":[],\"tag__and\":[],\"tag_slug__in\":[],\"tag_slug__and\":[],\"post_parent__in\":[],\"post_parent__not_in\":[],\"author__in\":[],\"author__not_in\":[],\"cache_results\":false,\"orderby\":\"relevance\",\"order\":\"DESC\",\"meta_query\":[],\"tax_query\":{\"relation\":\"AND\",\"0\":{\"taxonomy\":\"product_visibility\",\"field\":\"term_taxonomy_id\",\"terms\":[6],\"operator\":\"NOT IN\"}},\"wc_query\":\"product_query\",\"posts_per_page\":2,\"aws_query\":true,\"ignore_sticky_posts\":false,\"suppress_filters\":false,\"update_post_term_cache\":true,\"lazy_load_term_meta\":true,\"update_post_meta_cache\":true,\"nopaging\":false,\"comments_per_page\":\"50\",\"no_found_rows\":false,\"search_terms_count\":1,\"search_terms\":[\"" + keyword + "\"],\"search_orderby_title\":[\"mf21_posts.post_title LIKE '{fde74e118505842ef00e1050792a85fac50e43a991e5eb6378e146477d57db1d}" + keyword + "{fde74e118505842ef00e1050792a85fac50e43a991e5eb6378e146477d57db1d}'\"]}", "UTF-8"));
      payload.append("&ecs_ajax_settings=");
      payload.append(URLEncoder.encode("{\"current_page\":" + currentPage + ",\"max_num_pages\":" + maxNumPages + ",\"load_method\":\"lazyload\",\"widget_id\":\"" + widgetId + "\",\"post_id\":" + postId + ",\"theme_id\":" + themeId + "}", "UTF-8"));

      } catch (UnsupportedEncodingException e) {
         Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
      }

      return payload.toString();
   }

   private HashMap<String, String> crawlSearchParameters(Document doc) {
      HashMap<String, String> parameters = new HashMap<>();

      String jsonParameters = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".ecs-posts.elementor-posts-container.elementor-posts.elementor-grid.elementor-posts--skin-custom", "data-settings");
      JSONObject json = CrawlerUtils.stringToJson(jsonParameters);

      parameters.put("maxNumPages", json.optString("max_num_pages"));
      parameters.put("widgetId", json.optString("widget_id"));
      parameters.put("postId", json.optString("post_id"));
      parameters.put("themeId", json.optString("theme_id"));

      return parameters;
   }
}
