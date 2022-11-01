package br.com.lett.crawlernode.crawlers.ranking.keywords.riodejaneiro;

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
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Date: 27/01/21
 *
 * @author Fellype Layunne
 */

public class RiodejaneiroPrincesaniteroiCrawler extends CrawlerRankingKeywords {

   private String getLocation(){return session.getOptions().getString("filial");}
   public RiodejaneiroPrincesaniteroiCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      String url = "https://ecom.solidcon.com.br/api/v2/shop/produto/empresa/103/filial/"+getLocation()+"/GetProdutos";

      JSONArray products = fetchJsonFromApi(url);
      if (products != null && !products.isEmpty()) {
         for (Object e : products) {

            JSONObject skuInfo = (JSONObject) e;
            String internalPid = skuInfo.optString("cdProduto");
            String productUrl = "https://www.princesasupermercados.com.br/produtodetalhe/" + internalPid + "/False";
            String name = skuInfo.optString("nmProduto");
            String imgUrl = skuInfo.optString("urlFoto");
            Integer stock = skuInfo.optInt("qtDisponivel");
            boolean isAvailable = stock > 0;
            Integer price = isAvailable ? getPrice(skuInfo) : null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalPid)
               .setInternalPid(internalPid)
               .setName(name)
               .setImageUrl(imgUrl)
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

   private JSONArray fetchJsonFromApi(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA1LzA1L2lkZW50aXR5L2NsYWltcy9uYW1lIjoic29saWRjb24iLCJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA1LzA1L2lkZW50aXR5L2NsYWltcy9lbWFpbGFkZHJlc3MiOiJzb2xpZGNvbkBzb2xpZGNvbi5jb20uYnIiLCJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA1LzA1L2lkZW50aXR5L2NsYWltcy9uYW1laWRlbnRpZmllciI6IjM3NTNiYWEzLTVhZGYtNDY0Ni1hNTY5LTIxMmQxMzlhNjdmYyIsImV4cCI6MTk1NTA0OTg3OSwiaXNzIjoiRG9yc2FsV2ViQVBJIiwiYXVkIjoic29saWRjb24uY29tLmJyIn0.LxDewxZ-V_kXYjcl8sM9Z3nD5vkymfAv4mAWJXGx5o4");
      headers.put("content-type", "application/json; charset=utf-8");
      headers.put("accept", "application/json");

      String payload = "{\"Produto\":\"" + this.keywordWithoutAccents + "\"}";

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload)
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher(), new FetcherDataFetcher()), session, "post");

      return CrawlerUtils.stringToJsonArray(response.getBody());
   }

   private Integer getPrice(JSONObject object) {
      Double priceJson = object.optDouble("preco");
      if (priceJson != null) {
         return CommonMethods.doublePriceToIntegerPrice(priceJson, 0);
      }
      return null;
   }
}
