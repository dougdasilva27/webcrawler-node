package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.crawler.DiscoveryCrawlerSession;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SupertoninCrawler extends CrawlerRankingKeywords {

   private final String id_armazem = session.getOptions().optString("IdArmazem");

   public SupertoninCrawler(Session session) {
      super(session);
   }

   private JSONObject fetchProductsFromAPI() {
      JSONObject payload = new JSONObject();
      payload.put("descricao", this.keywordEncoded);
      payload.put("order", "MV");
      payload.put("pg", this.currentPage);
      payload.put("marcas", new JSONArray());
      payload.put("categorias", new JSONArray());
      payload.put("subcategorias", new JSONArray());
      payload.put("precoIni", 0);
      payload.put("precoFim", 0);
      payload.put("avaliacoes", new JSONArray());
      payload.put("num_reg_pag", 30);
      payload.put("visualizacao", "CARD");

      String url = "https://www.supertonin.com.br/api/busca";

      Map<String, String> headers = new HashMap<>();
      headers.put("Cookie", "ls.uid_armazem=" + id_armazem);
      headers.put("Content-Type", "application/json");
      headers.put("Accept", "application/json, text/plain, */*");

      BasicClientCookie cookie = new BasicClientCookie("ls.uid_armazem", id_armazem);
      cookie.setDomain("www.supertonin.com.br");
      cookie.setPath("/");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload.toString())
         .setCookies(Collections.singletonList(cookie))
         .build();

      JSONObject json = CrawlerUtils.stringToJson(new JsoupDataFetcher().post(session, request).getBody());

      return json;
   }

   @Override
   public void extractProductsFromCurrentPage() {
      this.pageSize = 30;
      this.log("Página " + this.currentPage);

      JSONObject json = fetchProductsFromAPI();

      if(this.currentPage == 1){
         setTotalProducts(json);
      }

      if (json.has("Produtos") && !json.optJSONArray("Produtos").isEmpty()) {
         JSONArray products = json.optJSONArray("Produtos");

         for (int i = 0; i < products.length(); i++) {
            JSONObject product = products.optJSONObject(i);

            String internalPid = product.optString("id_produto");
            String productUrl = "https://www.supertonin.com.br/produto/" + product.optString("str_link_produto");

            saveDataProduct(null, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalPid: " + internalPid + " - Url: " + productUrl);

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

   protected void setTotalProducts(JSONObject search) {
      if (search != null && search.has("Avaliacoes")) {
         JSONArray ratings = search.optJSONArray("Avaliacoes");
         this.totalProducts = ((JSONObject)ratings.opt(0)).optInt("int_qtd_produto");
         this.log("Total da busca: " + this.totalProducts);
      }
   }

   @Override
   protected boolean hasNextPage() {
      return this.arrayProducts.size() < this.totalProducts;
   }


}
