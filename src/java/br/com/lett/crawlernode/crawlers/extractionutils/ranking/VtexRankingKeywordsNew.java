package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public abstract class VtexRankingKeywordsNew extends CrawlerRankingKeywords {

   protected abstract String setHomePage();

   private final String homePage = setHomePage();

   protected VtexRankingKeywordsNew(Session session) {
      super(session);
      this.pageSize = 12;
   }

   @Override
   protected void extractProductsFromCurrentPage() {

      String url = homePage + "/api/catalog_system/pub/products/search/" + keywordEncoded.replace("+", "%20") + "?_from=" + ((currentPage - 1) * pageSize) +
         "&_to=" + ((currentPage) * pageSize);

      JSONArray products = fetchPage(url);

      for (Object object : products) {

         JSONObject product = (JSONObject) object;
         String productUrl = product.optString("link");
         String internalPid = product.optString("productId");

         saveDataProduct(null, internalPid, productUrl);

         this.log("Position: " + this.position + " - InternalId: " + null  + " - InternalPid: " + internalPid
            + " - Url: " + productUrl);

         if (this.arrayProducts.size() == productsLimit) {
            break;
         }
      }
   }

   protected JSONArray fetchPage(String url) {
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .build();

      Response response = dataFetcher.get(session, request);

      return CrawlerUtils.stringToJsonArray(response.getBody());
   }

   @Override
   protected boolean hasNextPage() {
      return ((arrayProducts.size() - 1) % pageSize - currentPage) < 0;
   }

}
