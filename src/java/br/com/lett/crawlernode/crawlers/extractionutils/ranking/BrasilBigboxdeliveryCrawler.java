package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.MathUtils;
import org.eclipse.jetty.util.ajax.JSON;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class BrasilBigboxdeliveryCrawler extends CrawlerRankingKeywords {

   private String STORE_ID = session.getOptions().optString("store_id");


   public BrasilBigboxdeliveryCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.log("Página " + this.currentPage);

      this.pageSize = 24;

      JSONArray products = scrapProductInfoFromAPI();


      if (!products.isEmpty()) {

         for (Object e : products) {

            JSONObject product = (JSONObject) e;

            String internalPid = product.optString("id");
            String internalId = internalPid;
            String productUrl = "https://www.bigboxdelivery.com.br/p/" +  product.optString("slug");
            String name = product.optString("name");
            String imageUrl = "https://assets.instabuy.com.br/ib.item.image.big/b-" + scrapImage(product.optJSONArray("images"));
            String priceString = String.valueOf(product.optDouble("min_price_valid"));
            int price = MathUtils.parseInt(priceString);
            boolean isAvailable = product.optBoolean("available_stock");

            //New way to send products to save data product
            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
               .build();

            saveDataProduct(productRanking);

            if (this.arrayProducts.size() == productsLimit)
               break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }



   private JSONArray scrapProductInfoFromAPI(){
      JSONArray products = new JSONArray();

      String url = "https://www.bigboxdelivery.com.br/apiv3/search?page="+this.currentPage+"&N="+this.pageSize+"&search="+this.keywordEncoded+"&store_id=" +STORE_ID;

      Request request = Request.RequestBuilder.create().setUrl(url).build();
      String response = this.dataFetcher.get(session,request).getBody();
      JSONObject json = CrawlerUtils.stringToJson(response);
      this.totalProducts = json.optInt("count");
      products = json.optJSONArray("data");

     return products;
   }

   private String scrapImage (JSONArray images){
      String primaryImage = "";

      for (Object o : images) {
         if (!images.isEmpty()) {
            primaryImage = (String) o;
         }
      }
      return primaryImage;
   }

   @Override
   protected void setTotalProducts() {
      this.log("Total da busca: " + this.totalProducts);
   }
}
