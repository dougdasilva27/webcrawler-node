package br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils;

import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public abstract class MexicoCornershopCrawlerRanking extends CrawlerRankingKeywords {

   private final String STORE_ID = getStoreId();
   private final String PRODUCT_STORE_ID = getProductStoreId();

   protected abstract String getStoreId();

   protected abstract String getProductStoreId();

   public MexicoCornershopCrawlerRanking(Session session) {
      super(session);
   }

   @Override
   public void extractProductsFromCurrentPage() {
      this.log("Página " + this.currentPage);

      String url = "https://cornershopapp.com/api/v1/branches/"
            + STORE_ID + "/search?query="
            + this.keywordEncoded;
      this.log("Link onde são feitos os crawlers: " + url);

      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
      JSONArray categories = CrawlerUtils.stringToJsonArray(this.dataFetcher.get(session, request).getBody());

      if (categories.length() > 0) {
         for (Object o : categories) {
            JSONObject category = (JSONObject) o;

            JSONArray products = category.has("products") ? category.getJSONArray("products") : new JSONArray();
            for (int i = 0; i < products.length(); i++) {
               JSONObject product = products.getJSONObject(i);

               String internalId = crawlInternalId(product);
               String productUrl = crawlProductUrl(internalId);

               saveDataProduct(internalId, null, productUrl);

               this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);

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

   @Override
   protected boolean hasNextPage() {
      return false;
   }

   private String crawlInternalId(JSONObject product) {
      String internalPid = null;

      if (product.has("id")) {
         internalPid = product.get("id").toString();
      }

      return internalPid;
   }

   private String crawlProductUrl(String id) {
      return "https://web.cornershopapp.com/store/" + PRODUCT_STORE_ID + "/featured/product/" + id;
   }


}
