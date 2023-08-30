package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.JSONUtils;
import org.apache.http.HttpHeaders;
import org.jooq.tools.json.JSONObject;
import org.json.JSONArray;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class BrasilLojamondelezCrawler extends CrawlerRankingKeywords {
   private String cookiePHPSESSID = null;

   public BrasilLojamondelezCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.HTTPCLIENT;
   }

   private final String CNPJ = session.getOptions().optString("cnpj");
   private final String AUTHORIZATION = session.getOptions().optString("Authorization");

   protected String fetchJson(String url) {

      JSONObject obj = new JSONObject();
      obj.put("Documento", this.CNPJ);
      obj.put("Keyword", this.keywordEncoded.replace("+", " "));

      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.CONTENT_TYPE, "application/json");
      headers.put(HttpHeaders.AUTHORIZATION, "Basic " + AUTHORIZATION);
      headers.put(HttpHeaders.REFERER, "https://www.lojamondelez.com.br/");
      headers.put(HttpHeaders.ACCEPT, "application/json");

      Request request = RequestBuilder.create()
         .setUrl(url)
         .setPayload(obj.toString())
         .setProxyservice(Arrays.asList(
            ProxyCollection.FIXED_IP_HAPROXY
         ))
         .setHeaders(headers)
         .build();

      Response response = this.dataFetcher.post(session, request);

      return response.getBody();
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.log("Página " + this.currentPage);

      this.pageSize = 24;
      String url = "https://www.lojamondelez.com.br/api/products/ranking";

      this.log("Link onde são feitos os crawlers: " + url);
      String response = fetchJson(url);
      JSONArray products = JSONUtils.stringToJsonArray(response);

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            this.totalProducts = products.length();
         }
         for (int i = (currentPage - 1) * pageSize; i < this.totalProducts && i < pageSize * currentPage; i++) {
            org.json.JSONObject product = (org.json.JSONObject) products.get(i);
            String productUrl = product.optString("product_url", null);
            String internalId = JSONUtils.getValueRecursive(product, "sku_variations.0.sku", String.class, "");
            String name = product.optString("name");
            String imageUrl = JSONUtils.getValueRecursive(product, "sku_variations[0].images.gg", String.class);
            Integer price = JSONUtils.getValueRecursive(product, "sku_variations[0].bestPrice", Integer.class);
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setName(name)
               .setPageNumber(this.currentPage)
               .setImageUrl(imageUrl)
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

   @Override
   protected boolean hasNextPage() {
      return (arrayProducts.size() < this.totalProducts);
   }
}
