package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

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

import java.util.HashMap;
import java.util.Map;

public class BrasilMadeiramadeiraCrawler extends CrawlerRankingKeywords {
   private static final String API_URL = "https://2zx4gyqyq3-dsn.algolia.net/1/indexes/*/queries?x-algolia-api-key=fb6a5bbf551a9aedb1760a4ddaf87ab4&x-algolia-application-id=2ZX4GYQYQ3";
   private static Integer productsTotal;

   public BrasilMadeiramadeiraCrawler(Session session) {
    super(session);
  }

   protected JSONObject fetch() {
      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");
      headers.put("Accept-Encoding","gzip, deflate, br");
      headers.put("Connection","keep-alive");
      Integer page = this.currentPage - 1;

      String payload = "{\n" +
         "    \"requests\": [\n" +
         "        {\n" +
         "            \"indexName\": \"vr-prod-poc-madeira-best-seller-desc\",\n" +
         "            \"params\": \"\",\n" +
         "            \"query\": \""+ this.keywordEncoded + "\",\n" +
         "            \"page\": " + page + "\n" +
         "        }\n" +
         "    ]\n" +
         "}";

      Request request = Request.RequestBuilder.create().setUrl(API_URL)
         .setPayload(payload)
         .setCookies(cookies)
         .setHeaders(headers)
         .setSendUserAgent(false)
         .build();

      Response response = new JsoupDataFetcher().post(session, request);
      JSONObject jsonBody = CrawlerUtils.stringToJson(response.getBody());
      this.productsTotal = JSONUtils.getValueRecursive(jsonBody, "results.0.nbSortedHits", Integer.class);

      return jsonBody;
   }

  @Override
  protected void extractProductsFromCurrentPage() throws MalformedProductException {
    this.pageSize = 36;

    this.log("Página " + this.currentPage);

     JSONObject jsonResponse = fetch();
     JSONArray products = JSONUtils.getValueRecursive(jsonResponse, "results.0.hits", JSONArray.class);

    if (products != null && !products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Object o : products) {
        JSONObject product = (JSONObject) o;

        String internalPid = product.optString("id_produto");
        String name = product.optString("nome");
        String productUrl = CrawlerUtils.completeUrl(product.optString("url"), "https:", "www.madeiramadeira.com.br");
        JSONObject priceDouble = JSONUtils.getValueRecursive(product, "mini_buy_box.0", JSONObject.class);
        Integer price = (int) Math.round((priceDouble.optDouble("preco_promocional") * 100));
        boolean isAvailable = !product.optBoolean("out_of_stock");
        JSONArray images = product.optJSONArray("imagens");
        String imgUrl = (images.length() > 0) ? images.get(0).toString() : null;

         RankingProduct productRanking = RankingProductBuilder.create()
            .setUrl(productUrl)
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

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
        + this.arrayProducts.size() + " produtos crawleados");
  }

   @Override
   protected boolean hasNextPage() {
      if (this.productsTotal != null && this.arrayProducts.size() < this.productsTotal) {
         return true;
      }
      return false;
   }
}
