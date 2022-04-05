package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class ArgentinaCordiezCrawler  extends CrawlerRankingKeywords {

   public ArgentinaCordiezCrawler(Session session) {
      super(session);
   }

   private JSONArray getProductsFromApi(){

      //String urlApi = "https://www.cordiez.com.ar/api/catalog_system/pub/products/search?ft="+this.keywordEncoded;

      String urlApi = "https://www.cordiez.com.ar/api/catalog_system/pub/products/search?ft="+  this.keywordEncoded + "&_from=0&_to=17&O=OrderByScoreDESC";

      Request request =  Request.RequestBuilder.create().setUrl(urlApi).build();
      String resp = this.dataFetcher.get(session,request).getBody();

      return JSONUtils.stringToJsonArray(resp);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      JSONArray products = getProductsFromApi();

      if( products.length() > 0) {
         for (Object o : products) {
            JSONObject product = (JSONObject) o;

            String internalId = JSONUtils.getValueRecursive(product,"items.0.images.0.itemId", String.class);
            String productUrl = product.optString("link");
            String name = product.optString("productTitle");

            String imgUrl = JSONUtils.getValueRecursive(product,"items.0.images.0.imageUrl", String.class);
            Double priceDouble = JSONUtils.getValueRecursive(product,"items.0.sellers.0.commertialOffer.Price", Double.class);
            Integer price = Math.toIntExact(Math.round(priceDouble * 100));
            boolean isAvailable = JSONUtils.getValueRecursive(product,"items.0.sellers.0.commertialOffer.IsAvailable", Boolean.class);;
            if(!isAvailable){
               price=null;
            }


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

      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }
   }
}
