package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
      headers.put("Accept", "application/json, text/plain, */*");
      headers.put("Content-Type", "application/json;charset=UTF-8");
      headers.put("Accept-Encoding", "gzip, deflate, br");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload.toString())
         .mustSendContentEncoding(false)
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR
         ))

         .build();
      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher(), new FetcherDataFetcher()), session, "post");
      JSONObject json = CrawlerUtils.stringToJson(response.getBody());

      return json;
   }

   @Override
   public void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 30;
      this.log("Página " + this.currentPage);

      JSONObject json = fetchProductsFromAPI();

      if (this.currentPage == 1) {
         setTotalProducts(json);
      }

      if (json.has("Produtos") && !json.optJSONArray("Produtos").isEmpty()) {
         JSONArray products = json.optJSONArray("Produtos");

         for (int i = 0; i < products.length(); i++) {
            JSONObject product = products.optJSONObject(i);

            String internalPid = product.optString("id_produto");
            String productUrl = "https://www.supertonin.com.br/produto/" + product.optString("str_link_produto");
            String name = product.optString("str_nom_produto");
            String imgUrl = product.optString("str_img_path_cdn");
            int price = JSONUtils.getPriceInCents(product, "mny_vlr_produto_por");
            boolean isAvailable = !product.optBoolean("bit_esgotado");

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(null)
               .setInternalPid(internalPid)
               .setImageUrl(imgUrl)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);

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
         this.totalProducts = ((JSONObject) ratings.opt(0)).optInt("int_qtd_produto");
         this.log("Total da busca: " + this.totalProducts);
      }
   }

   @Override
   protected boolean hasNextPage() {
      return this.arrayProducts.size() < this.totalProducts;
   }


}
