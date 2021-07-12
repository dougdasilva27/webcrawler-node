package br.com.lett.crawlernode.crawlers.ranking.keywords.guatemala;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class GuatemalaCemacoCrawler extends CrawlerRankingKeywords {


   public GuatemalaCemacoCrawler(Session session) {
      super(session);
   }


   private JSONObject searchJson() {
      Request request = Request.RequestBuilder.create()
         .setUrl("https://cemacoaws-search.celebros.com/UiSearch/DoSearch?pageSize=" + this.productsLimit + "&profile=SiteDefault&query=" + this.keywordEncoded + "&siteId=Cemaco")
         .build();

      String response = dataFetcher.get(session, request).getBody();

      JSONObject result = CrawlerUtils.stringToJson(response);
      return result.optJSONObject("DoSearchResult");
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException {

      JSONObject searchResult = searchJson();

      if (searchResult != null && searchResult.has("Products")) {

         if (this.totalProducts == 0) {
            this.totalProducts = searchResult.optInt("ProductsCount");
         }

         for (Object object : searchResult.optJSONArray("Products")) {
            JSONObject products = (JSONObject) object;

            String internalId = products.optString("sku");
            String url = getUrl(products);

            saveDataProduct(internalId, internalId, url);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalId + " - Url: " + url);
         }


      } else {
         log("keyword sem resultado");
      }
   }

   private String getUrl(JSONObject products) {
      String url = null;
      String parametersUrl = products.getString("ProductPageUrl");

      if (parametersUrl != null) {
         url = "https://www.cemaco.com/" + parametersUrl;
      }

      return url;
   }


}
