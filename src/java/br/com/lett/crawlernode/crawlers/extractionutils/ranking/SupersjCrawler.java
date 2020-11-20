package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

abstract public class SupersjCrawler extends CrawlerRankingKeywords {

   private static final String PRODUCT_PAGE = "www.supersj.com.br/produto";
   private static final String API = "https://www.supersj.com.br/api/busca";
   private int totalPages;
   private static final List<Cookie> COOKIES = new ArrayList<>();

   public SupersjCrawler(Session session) {
      super(session);
   }

   protected abstract String getLocationId();

   private JSONObject fetchAPI(){

      JSONObject payload = new JSONObject();
      payload.put("descricao", this.keywordEncoded);
      payload.put("order", "MV");
      payload.put("pg", this.currentPage);
      payload.put("marcas", Arrays.asList());
      payload.put("categorias", Arrays.asList());
      payload.put("subcategorias", Arrays.asList());
      payload.put("precoIni", 0);
      payload.put("precoFim", 0);
      payload.put("avaliacoes", Arrays.asList());
      payload.put("num_reg_pag", pageSize);
      payload.put("visualizacao", "CARD");

      BasicClientCookie cookie = new BasicClientCookie("ls.uid_armazem", getLocationId());
      cookie.setDomain("www.supersj.com.br");
      cookie.setPath("/");
      COOKIES.add(cookie);

      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "application/json");
      headers.put("content-type", "application/json");

      Request request = Request.RequestBuilder.create().setUrl(API).setHeaders(headers).setCookies(COOKIES).setPayload(payload.toString())
         .build();

      return JSONUtils.stringToJson(this.dataFetcher.post(session, request).getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() {

      this.log("Página " + this.currentPage);
      JSONObject apiResponse = fetchAPI();
      JSONArray productsArray = apiResponse.optJSONArray("Produtos");

      if (productsArray != null && !productsArray.isEmpty()) {
         for (int i = 0; i< productsArray.length(); i++) {

            JSONObject product = productsArray.getJSONObject(i);
            String internalId = String.valueOf(product.optInt("id_produto"));
            String productUrl = CrawlerUtils.completeUrl(product.optString("str_link_produto"), "https", PRODUCT_PAGE);

            if(i==0){
               this.totalPages = product.optInt("ult_pag");
            }

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
   }

   @Override
   protected boolean hasNextPage(){
      return this.currentPage != this.totalPages;
   }
}
