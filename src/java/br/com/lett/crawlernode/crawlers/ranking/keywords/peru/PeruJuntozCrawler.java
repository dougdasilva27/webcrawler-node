package br.com.lett.crawlernode.crawlers.ranking.keywords.peru;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class PeruJuntozCrawler extends CrawlerRankingKeywords {

   public PeruJuntozCrawler(Session session) {
      super(session);
   }

   private JSONObject fetchJSON() {
      String url = "https://juntoz.com/proxy/products/catalog/v2?keywords=" + this.keywordEncoded + "&orderBy=rating-desc&top=28&skip=" + this.arrayProducts.size();
      this.log("Link onde são feitos os crawlers: " + url);

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .build();

      Response response = dataFetcher.get(session, request);
      return CrawlerUtils.stringToJson(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      JSONObject json = fetchJSON();

      JSONArray results = json.optJSONArray("products");

      if (results != null && !results.isEmpty()) {
         if (currentPage == 1) {
            this.totalProducts = json.optInt("totalCount");
         }

         for (Object prod : results) {
            JSONObject product = (JSONObject) prod;

            String internalPid = product.optString("productId");
            String internalId = product.optString("sku");
            String productUrl = "https://juntoz.com/producto/" + product.optString("id");
            String name = product.optString("name");
            int price = product.optInt("specialPrice", 0);
            boolean isAvailable = price != 0;

            //New way to send products to save data product
            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
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

}
