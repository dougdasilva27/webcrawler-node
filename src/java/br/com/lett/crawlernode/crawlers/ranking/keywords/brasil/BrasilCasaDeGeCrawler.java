package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class BrasilCasaDeGeCrawler extends CrawlerRankingKeywords {
   private final String HOME_PAGE = "https://casadege.com.br/";

   public BrasilCasaDeGeCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 32;
      String url = "https://api-marketplace.casadege.com.br/api/collective/client/marketplace/casadege.com.br/core/product?per_page=32&page=" + this.currentPage +"&name=" + this.keywordEncoded;
      JSONObject productsResponse = fetchJSONObject(url);
      JSONArray products = productsResponse.optJSONArray("data");

      if(products != null && products.length() > 0) {
         for (Object product : products) {
            if(product instanceof JSONObject) {
               JSONObject productJson = (JSONObject) product;
               String internalId = productJson.optString("code_sku");
               String internalPid = productJson.optString("id");
               String name = productJson.optString("name");
               String productUrl = scrapProductUrl(productJson);
               String image = productJson.optString("img_spotlight");
               int priceInCents = (int) Math.round(productJson.optDouble("price", 0d)  * 100);
               boolean available = productJson.optInt("inventory_quantity") > 0;

               RankingProduct productRanking = RankingProductBuilder.create()
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setUrl(productUrl)
                  .setImageUrl(image)
                  .setAvailability(available)
                  .setPriceInCents(priceInCents)
                  .build();

               saveDataProduct(productRanking);
            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultados!");
      }

   }

   private String scrapProductUrl(JSONObject productJson) {
      String productPath = productJson.optString("url");
      return HOME_PAGE + productPath;
   }

   @Override
   protected boolean hasNextPage() {
      return (arrayProducts.size() % pageSize - currentPage) < 0;
   }
}
