package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpHeaders;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilKochCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "https://www.superkoch.com.br/";

   public BrasilKochCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 30;
      this.log("Página " + this.currentPage);
      JSONArray products = fetchProductsApi();

      if (!products.isEmpty()) {
         for (Object o : products) {
            JSONObject product = (JSONObject) o;
            String productUrl = HOME_PAGE + product.optString("str_link_produto");;
            String internalPid = product.optString("id_produto", null);

            saveDataProduct(null, internalPid, productUrl);

            this.log(
                  "Position: " + this.position +
                        " - InternalId: " + null +
                        " - InternalPid: " + internalPid +
                        " - Url: " + productUrl);

            if (this.arrayProducts.size() == productsLimit)
               break;
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
      return this.arrayProducts.size() >= this.pageSize
            && (this.arrayProducts.size() / this.currentPage) >= this.pageSize;
   }

   private JSONArray fetchProductsApi() {
      JSONArray products = new JSONArray();

      JSONObject payload = new JSONObject();
      payload.put("descricao", this.keywordWithoutAccents);
      payload.put("order", "MV");
      payload.put("pg", this.currentPage);
      payload.put("marcas", new JSONArray());
      payload.put("categorias", new JSONArray());
      payload.put("subcategorias", new JSONArray());
      payload.put("precoIni", 0);
      payload.put("precoFim", 0);
      payload.put("avaliacoes", new JSONArray());
      payload.put("num_reg_pag", this.pageSize);
      payload.put("visualizacao", "CARD");

      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.CONTENT_TYPE, "application/json");

      Request request = RequestBuilder.create()
            .setHeaders(headers)
            .setUrl("https://www.superkoch.com.br/api/busca")
            .setPayload(payload.toString())
            .build();

      JSONObject api = CrawlerUtils.stringToJson(this.dataFetcher.post(session, request).getBody());
      if (api.has("Produtos") && api.get("Produtos") instanceof JSONArray) {
         products = api.getJSONArray("Produtos");
      }

      return products;
   }
}
