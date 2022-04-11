package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.JavanetDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class BrasilSemardriveCrawler extends CrawlerRankingKeywords {

   public BrasilSemardriveCrawler(Session session) {
      super(session);
   }

   protected String getProductsList() {
      String url = "https://www.semarentrega.com.br/ccstoreui/v1/search?suppressResults=false&searchType=simple&No=1&Nrpp=24&Ntt=" + keywordEncoded + "&page=" + (currentPage - 1);

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setProxyservice(Arrays.asList(ProxyCollection.NETNUT_RESIDENTIAL_BR))
         .build();

      String response = dataFetcher.get(session, request).getBody();

      return response;

   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      String response = getProductsList();
      JSONObject reponseJson = CrawlerUtils.stringToJson(response);
      this.totalProducts = reponseJson.optJSONObject("resultsList").optInt("totalNumRecs");
      if (totalProducts > 0) {
         JSONArray productsList = reponseJson.getJSONObject("resultsList").getJSONArray("records");
         for (Object arrayOfArrays : productsList) {
            JSONObject jsonInfo = (JSONObject) arrayOfArrays;
            JSONArray records = jsonInfo.getJSONArray("records");
            String productUrl = "https://www.semarentrega.com.br" + records.optQuery("/0/attributes/product.route/0").toString();
            String internalId = jsonInfo.optQuery("/attributes/product.repositoryId/0").toString();
            String name = records.optQuery("/0/attributes/sku.displayName/0").toString();
            String imgUrl = "https://www.semarentrega.com.br" + records.optQuery("/0/attributes/product.primaryFullImageURL/0").toString();

            String availability = records.optQuery("/0/attributes/sku.availabilityStatus/0").toString();
            boolean isAvailable = availability.isEmpty() || !availability.contains("OUTOFSTOCK");
            Integer price = null;
            if(isAvailable == true){
               String priceText = (String) records.optQuery("/0/attributes/product.listPrice/0");
               if(priceText != null && !priceText.isEmpty()){
                  Double priceDouble = Double.parseDouble(priceText) * 100;
                  price = priceDouble.intValue();
               }
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

         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

   }
}

