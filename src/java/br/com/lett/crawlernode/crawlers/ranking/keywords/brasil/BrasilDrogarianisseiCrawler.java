package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;

public class BrasilDrogarianisseiCrawler extends CrawlerRankingKeywords {

   private static final String API = "https://www.farmaciasnissei.com.br/pesquisa/pesquisar";

   private static final String API_TOKEN = "csrfmiddlewaretoken=4rkrXGRKYg5D1sb1x0LGDQKKQTX88euya6hdaULXA78wH81gxPzGYnWahiNf0Y4Z";

   private static final String API_COOKIE = "csrftoken=vQ4QPYu1Y9rq1irLMF2jjjvJ5eWGV8V0Bv1C2coeA0ujHYh0MuQjEQH9wDMNNSvr;";

   public BrasilDrogarianisseiCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {

      JSONObject productsJson = fetchJSONObject(API);

      if (this.totalProducts == 0) {
         this.totalProducts = productsJson.optInt("quantidade");
      }

      JSONArray products = JSONUtils.getJSONArrayValue(productsJson,"produtos");

      if (!products.isEmpty()) {

         for (Object e: products) {

            JSONObject product = (JSONObject) e;

            String internalId = product.optString("_id");

            String productUrl = scrapProductUrl(product);

            saveDataProduct(internalId, internalId, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalId + " - Url: " + productUrl);
            if (this.arrayProducts.size() == productsLimit)
               break;

         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   protected JSONObject fetchJSONObject(String url){

      String payload = API_TOKEN + "&termo=pesquisa%2F" + this.keywordEncoded + "&pagina=" + this.currentPage +"&is_termo=true";

      HashMap<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/x-www-form-urlencoded");
      headers.put("referer", "https://www.farmaciasnissei.com.br/pesquisa/absorvente");
      headers.put("cookie", API_COOKIE);

      Request request = Request.RequestBuilder.create().setPayload(payload).setHeaders(headers).setCookies(cookies).setUrl(url).build();
      String response = dataFetcher.post(session, request).getBody();

      return CrawlerUtils.stringToJson(response);
   }

   private String scrapProductUrl(JSONObject product){

      String url = null;
      JSONObject source = product.optJSONObject("_source");

      if(source != null){

         String urlPart = source.optString("url_produto");

         if(urlPart != null){
            url = CrawlerUtils.completeUrl(urlPart, "https", "www.farmaciasnissei.com.br");
         }
      }

      return url;
   }

}
