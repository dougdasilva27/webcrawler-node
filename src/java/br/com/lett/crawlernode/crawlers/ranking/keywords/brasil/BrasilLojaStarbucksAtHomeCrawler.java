package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
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


import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class BrasilLojaStarbucksAtHomeCrawler extends CrawlerRankingKeywords {
   public BrasilLojaStarbucksAtHomeCrawler(Session session) {
      super(session);
   }

   protected JSONObject fetchDocument() {
      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/json");

      Request request = Request.RequestBuilder.create()
         .setUrl("https://www.starbucksathome.com/br/rest/V1/wj/search?searchCriteria[requestName]=quick_search_container&searchCriteria[filterGroups][0][filters][0][field]=search_term&searchCriteria[filterGroups][0][filters][0][value]="+ this.keywordWithoutAccents)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.LUMINATI_SERVER_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_BR))
         .build();

      Response response = CrawlerUtils.retryRequest(request, session, new JsoupDataFetcher(), true);

      return JSONUtils.stringToJson(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 12;
      this.log("Página " + this.currentPage);

      JSONObject json = fetchDocument();

      if (json != null && !json.isEmpty()) {
         JSONArray productsArray = JSONUtils.getJSONArrayValue(json, "search");
         this.totalProducts = productsArray.length();

         for (Object product : productsArray) {
            JSONObject productJson = (JSONObject) product;

            if(productJson != null) {
               String internalId = Integer.toString(JSONUtils.getIntegerValueFromJSON(productJson, "id", 0));
               String productUrl = JSONUtils.getStringValue(productJson, "url");
               String name = JSONUtils.getStringValue(productJson, "name");
               String imageUrl = JSONUtils.getStringValue(productJson, "image");
               Integer price = JSONUtils.getPriceInCents(productJson, "price");
               boolean isAvailable = price != null;

               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(internalId)
                  .setName(name)
                  .setPriceInCents(price)
                  .setAvailability(isAvailable)
                  .setImageUrl(imageUrl)
                  .build();

               saveDataProduct(productRanking);
            }
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

}
