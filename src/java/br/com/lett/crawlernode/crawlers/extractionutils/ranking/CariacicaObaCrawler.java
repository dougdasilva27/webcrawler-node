package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class CariacicaObaCrawler extends CrawlerRankingKeywords {


   public CariacicaObaCrawler(Session session) {
      super(session);
   }

   public JSONObject crawlApi() {

      String apiUrl = "https://www.superoba.com.br/api/busca";
      String payload = "{\"descricao\":\"" + this.keywordEncoded + "\",\"order\":\"MV\",\"pg\":" + this.currentPage + ",\"marcas\":[],\"categorias\":[],\"subcategorias\":[],\"precoIni\":0,\"precoFim\":0,\"avaliacoes\":[],\"num_reg_pag\":30,\"visualizacao\":\"CARD\"}";

      Map<String, String> headers = new HashMap<>();
      headers.put("Cookie", "ls.uid_armazem=" + session.getOptions().optString("id_armazen"));
      headers.put("Content-Type", "application/json;charset=UTF-8");
      headers.put("Accept", "application/json, text/plain, */*");

      Request request = Request.RequestBuilder.create()
         .setUrl(apiUrl)
         .setHeaders(headers)
         .setPayload(payload)
         .build();
      String content = new FetcherDataFetcher()
         .post(session, request)
         .getBody();

      return CrawlerUtils.stringToJson(content);

   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {

      this.log("Página " + this.currentPage);

      JSONObject json = crawlApi();

      JSONArray productsArray = json.optJSONArray("Produtos");

      if (productsArray != null && !productsArray.isEmpty()) {

         for (Object e : productsArray) {
            if (this.totalProducts == 0) {
               setTotalProducts(json);
            }

            JSONObject product = (JSONObject) e;

            if (product != null) {

               String internalId = product.optString("id_produto");
               String productUrl = CrawlerUtils.completeUrl(product.optString("str_link_produto"), "https", "www.superoba.com.b");
               String name = product.optString("str_nom_produto");
               String imageUrl = product.optString("str_img_path");
               int price = CommonMethods.doublePriceToIntegerPrice(JSONUtils.getDoubleValueFromJSON(product, "mny_vlr_produto_por", true), 0);
               boolean isAvailable = price != 0;

               //New way to send products to save data product
               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(internalId)
                  .setName(name)
                  .setPriceInCents(price)
                  .setAvailability(isAvailable)
                  .setImageUrl(imageUrl)
                  .build();

               saveDataProduct(productRanking);
               if (this.arrayProducts.size() == productsLimit) {
                  break;
               }
            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }


   protected void setTotalProducts(JSONObject json) {
      this.totalProducts = JSONUtils.getValueRecursive(json, "Avaliacoes.0.int_qtd_produto", Integer.class);
      this.log("Total da busca: " + this.totalProducts);
   }

}
