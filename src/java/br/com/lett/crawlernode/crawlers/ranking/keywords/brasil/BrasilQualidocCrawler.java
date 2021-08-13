package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class BrasilQualidocCrawler extends CrawlerRankingKeywords {

   public BrasilQualidocCrawler(Session session) {
      super(session);
   }

   @Override
   protected JSONObject fetchJSONObject(String url) {
      Map<String, String> headers = new HashMap<>();

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .setHeaders(headers)
         .build();

      Response response = this.dataFetcher.get(session, request);

      return JSONUtils.stringToJson(response.getBody());
   }

   @Override
   public void extractProductsFromCurrentPage() {
      this.pageSize = 12;

      this.log("Página " + this.currentPage);

      //I didnt find a way to get this ID's. But the id is random, if you change it following the same number of characters, the request works the same.
      String url = "https://www.qualidoc.com.br/ccstoreui/v1/search?Nrpp=12&visitorId=1015BD0eA19213YWlCVNEDdqiZaY4AgiFltmRqZe1bSxgMkDBE3&visitId=e258768%3A17b3f82c1fb%3A-6c6e-4094297612&totalResults=true&" +
         "No=" + this.arrayProducts.size() +
         "&searchType=simple&Nr=AND(product.active%3A1)&Ntt=" + this.keywordEncoded;
      JSONObject search = fetchJSONObject(url);

      JSONArray products = JSONUtils.getValueRecursive(search, "resultsList.records", JSONArray.class);

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts(search);
         }

         for (int i = 0; i < products.length(); i++) {
            JSONObject product = products.optJSONObject(i);
            String productUrl = "https://www.qualidoc.com.br" + JSONUtils.getValueRecursive(product, "records,0,attributes,product.route,0", ",", String.class, null);
            String internalPid = JSONUtils.getValueRecursive(product, "records,0,attributes,product.repositoryId,0", ",", String.class, null);

            saveDataProduct(null, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);

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
      this.totalProducts = JSONUtils.getValueRecursive(search , "resultsList.totalNumRecs", Integer.class);
      this.log("Total da busca: " + this.totalProducts);
   }
}
