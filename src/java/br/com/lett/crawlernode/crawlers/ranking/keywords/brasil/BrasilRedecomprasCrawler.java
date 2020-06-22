package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;

public class BrasilRedecomprasCrawler extends CrawlerRankingKeywords {

   public BrasilRedecomprasCrawler(Session session) {
      super(session);
   }



   @Override
   protected void extractProductsFromCurrentPage() {

      this.pageSize = 30;
      this.log("Página " + this.currentPage);


      String url = "https://delivery.redecompras.com/api/busca";

      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/json");

      JSONObject payload = new JSONObject();
      payload.put("descricao", this.keywordWithoutAccents);
      payload.put("order", "MV");
      payload.put("pg", this.currentPage);
      payload.put("marcas", new JSONArray());
      payload.put("categorias", new JSONArray());
      payload.put("subcategorias", new JSONArray());
      payload.put("precoIni", "0");
      payload.put("precoFim", "0");
      payload.put("avaliacoes", new JSONArray());
      payload.put("num_reg_pag", "30");
      payload.put("visualizacao", "CARD");

      Request request = RequestBuilder.create().setUrl(url).setHeaders(headers).setPayload(payload.toString()).build();
      JSONObject json = CrawlerUtils.stringToJson(this.dataFetcher.post(session, request).getBody());

      JSONArray arr = JSONUtils.getJSONArrayValue(json, "Produtos");

      this.log("Link onde são feitos os crawlers: " + url);

      if (!arr.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Object productInfo : arr) {

            JSONObject info = (JSONObject) productInfo;

            String internalId = info.optString("id_produto");
            String internalPid = internalId;
            String productUrl = CrawlerUtils.completeUrl(info.optString("str_link_produto"), "https://", "delivery.redecompras.com/produto/");

            saveDataProduct(internalId, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " +
                  internalPid + " - Url: " + productUrl);
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

}
