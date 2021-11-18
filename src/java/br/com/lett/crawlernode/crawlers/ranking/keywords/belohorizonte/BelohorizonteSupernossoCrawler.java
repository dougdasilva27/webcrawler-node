package br.com.lett.crawlernode.crawlers.ranking.keywords.belohorizonte;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Map;

public class BelohorizonteSupernossoCrawler extends CrawlerRankingKeywords {

   public BelohorizonteSupernossoCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 24;

      this.log("Página " + this.currentPage);

      JSONObject json = fetchApi();
      JSONArray products = json.optJSONArray("products");

      if (products != null && !products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts(json);
         }

         for (Object obj : products) {
            JSONObject product = (JSONObject) obj;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(product.optString("url"))
               .setInternalPid(product.optString("id"))
               .setName(product.optString("name"))
               .setPriceInCents(scrapPrice(product))
               .setAvailability(product.optString("status").equals("AVAILABLE"))
               .setImageUrl((String) product.optQuery("/images/default"))
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

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private Integer scrapPrice(JSONObject product) {
      double price = product.optDouble("price");
      Integer priceInCents = null;
      if (price != 0.0) {
         priceInCents = Integer.parseInt(Double.toString(price).replace(".", ""));
      }
      return priceInCents;
   }

   private void setTotalProducts(JSONObject json) {
      this.totalProducts = json.optInt("size", 0);
      this.log("Total da busca: " + this.totalProducts);
   }

   private JSONObject fetchApi() {
      String url = "https://api.linximpulse.com/engage/search/v3/search?apiKey=supernossoemcasa&page=" + this.currentPage + "&resultsPerPage=24&terms=" + this.keywordEncoded + "&salesChannel=1";
      this.log("Link onde são feitos os crawlers: " + url);

      Map<String, String> headers = new HashMap<>();
      headers.put("Origin", "https://www.supernossoemcasa.com.br");

      Request request = Request.RequestBuilder.create()
         .setHeaders(headers)
         .setUrl(url)
         .build();

      String response = dataFetcher.get(session, request).getBody();
      return CrawlerUtils.stringToJson(response);
   }
}
