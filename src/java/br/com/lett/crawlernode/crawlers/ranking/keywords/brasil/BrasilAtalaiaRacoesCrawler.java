package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.MathUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilAtalaiaRacoesCrawler extends CrawlerRankingKeywords {
   public BrasilAtalaiaRacoesCrawler(Session session) {
      super(session);
   }

   JSONArray products = null;

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      String url = "https://superon.lifeapps.com.br/api/v1/app/777d0060-40c1-11ed-a1e1-750c7c041041cc64548c0cbad6cf58d4d3bbd433142b/listaprodutosf/c9ecd9f3-b54f-4ab9-b856-62df3df6960c?sk=" + this.keywordEncoded + "&page=" + (this.currentPage - 1) + "&canalVenda=WEB&tipoentrega=DELIVERY&tagsPesquisa=";
      JSONObject json = fetchJSONObject(url);
      products = JSONUtils.getValueRecursive(json, "dados", JSONArray.class);
      if (products != null && !products.isEmpty()) {
         for (Object e : products) {
            JSONObject product = (JSONObject) e;
            String slug = product.optString("slug");
            String productUrl = slug != null && !slug.isEmpty() ? "https://loja.atalaiaracoes.com.br/store/produto/" + slug : null;
            String internalId = product.optString("idproduto");
            String name = product.optString("nome");
            String id = product.optString("id");
            String imgUrl = id != null && !id.isEmpty() ? "https://content.lifeapps.com.br/superon/imagens/" + id + ".jpg" : null;
            Integer price = getPrice(product);
            String stock = product.optString("maximo_disponivel");
            boolean isAvailable = checkAvailability(stock);

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
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
      }
   }

   private Integer getPrice(JSONObject product) {
      Double priceDouble = product.optDouble("preco");
      if (priceDouble != null) {
         priceDouble = priceDouble * 100;
         return CommonMethods.doublePriceToIntegerPrice(priceDouble, 0);
      }
      return null;
   }

   private boolean checkAvailability(String stock) {
      if (!stock.isEmpty()) {
         stock = stock.replaceAll("\\.", "");
         if (stock != null) {
            Integer stockInt = Integer.parseInt(stock);
            if (stockInt > 0) {
               return true;
            }
         }
      }
      return false;
   }

   @Override
   protected boolean hasNextPage() {
      return products.isEmpty() ? false : true;
   }


   @Override
   protected JSONObject fetchJSONObject(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("authority", "superon.lifeapps.com.br");
      headers.put("x-idfornecedor", "[\"c9ecd9f3-b54f-4ab9-b856-62df3df6960c\"]");
      headers.put("origin", "https://loja.atalaiaracoes.com.br");
      headers.put("referer", "https://loja.atalaiaracoes.com.br/");
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build();
      Response response = this.dataFetcher.get(session, request);
      return JSONUtils.stringToJson(response.getBody());
   }

}
