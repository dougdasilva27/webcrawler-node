package br.com.lett.crawlernode.crawlers.ranking.keywords.campinas;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
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
import br.com.lett.crawlernode.util.JSONUtils;
import org.apache.http.HttpHeaders;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CampinasDiscampCrawler extends CrawlerRankingKeywords {

   public CampinasDiscampCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.JSOUP;
   }

   Integer pageSize = 30;
   String homePage = "https://loja.vrsoft.com.br/discamp/produto/570/";

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      JSONObject json = fetchJSONObject("https://api.vrconnect.com.br/loja-virtual/browser/v1.05/buscarFeedProdutos");
      if (json != null) {
         totalProducts = JSONUtils.getValueRecursive(json, "retorno.total_itens", Integer.class);
         JSONArray products = JSONUtils.getValueRecursive(json, "retorno.itens", JSONArray.class);
         if (products != null && !products.isEmpty()) {
            for (Object obj : products) {
               JSONObject product = (JSONObject) obj;
               String internalId = product.optString("id_produto");
               String name = product.optString("nome");
               Boolean available = checkAvailability(product.optString("qtdmin", null));
               String productUrl = getUrl(product);
               String imageUrl = getImage(product);
               Integer price = getPrice(product);
               RankingProduct productRanking = RankingProductBuilder.create()
                  .setInternalId(internalId)
                  .setName(name)
                  .setAvailability(available)
                  .setUrl(productUrl)
                  .setImageUrl(imageUrl)
                  .setPriceInCents(price)
                  .build();
               saveDataProduct(productRanking);
               if (this.arrayProducts.size() == productsLimit) {
                  break;
               }
            }
         }
      }
   }

   private String getUrl(JSONObject product) {
      String urlKey = product.optString("codigo");
      if ((urlKey) != null && !urlKey.isEmpty()) {
         return homePage + urlKey;
      }
      return null;
   }

   private String getImage(JSONObject product) {
      String image = product.optString("imgid");
      if (image != null && !image.isEmpty()) {
         return "https://estaticos.nweb.com.br/imgs/nprodutos/t-" + image + ".jpg";
      }
      return null;
   }

   private Integer getPrice(JSONObject product) {
      Double priceDouble = JSONUtils.getValueRecursive(product, "preco", Double.class);
      if (priceDouble != null) {
         return CommonMethods.doublePriceToIntegerPrice(priceDouble, 0);
      } else {
         Integer priceInt = JSONUtils.getValueRecursive(product, "preco", Integer.class);
         if (priceInt != null) {
            return priceInt * 100;
         }
      }
      return null;
   }

   private Boolean checkAvailability(String stock) {
      if (stock != null && !stock.isEmpty()) {
         return true;
      }
      return false;
   }

   @Override
   protected JSONObject fetchJSONObject(String url) {
      String payload = "{\"cabecalho\":{\"loja\":\"570\"},\"parametros\":{\"buscar\":" + '"' + this.location + '"' + " ,\"paginacao\": " + ((currentPage - 1) * pageSize) + "}}";
      Map<String, String> headers = new HashMap<>();
      String token = "Bearer uoteK1SjWL0LNH5lsgWUJUobNqIjp6kivELR0rbBroGfa73WvLg1itsS2wuU2umjvyfAbbooMIf6Kt542ZPcw0upBM7dJ20UxV2o";
      headers.put("origin", "https://loja.vrsoft.com.br");
      headers.put("Authorization", token);
      headers.put(HttpHeaders.CONTENT_TYPE, "application/json");
      headers.put("authority", "api.vrconnect.com.br");
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload)
         .build();
      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new FetcherDataFetcher(), new JsoupDataFetcher(), new ApacheDataFetcher()), session, "post");
      return JSONUtils.stringToJson(response.getBody());
   }
}
