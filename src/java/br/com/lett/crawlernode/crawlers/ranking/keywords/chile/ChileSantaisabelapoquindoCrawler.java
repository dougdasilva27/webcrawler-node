package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

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
import java.util.List;
import java.util.Map;

public class ChileSantaisabelapoquindoCrawler extends CrawlerRankingKeywords {

   public ChileSantaisabelapoquindoCrawler(Session session) {
      super(session);
   }
   private String account = session.getOptions().optString("account","");
   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.log("Página " + this.currentPage);

      this.pageSize = 40;

      JSONObject searchedJson = getJSONFromAPI();

      JSONArray products = JSONUtils.getValueRecursive(searchedJson, "products", JSONArray.class);

      if (products != null && products.length() > 0) {

         if (this.totalProducts == 0) {

            setTotalProducts(searchedJson);

         }

         for (Object object : products) {

            JSONObject product = (JSONObject) object;

            String internalId = JSONUtils.getValueRecursive(product, "items.0.itemId", String.class);
            String productUrl = CrawlerUtils.completeUrl(product.optString("linkText"), "https", "www.santaisabel.cl") + "/p";
            String name = product.optString("productName");
            String imgUrl = JSONUtils.getValueRecursive(product, "items.0.images.0.imageUrl", String.class);
            Boolean isAvailable = JSONUtils.getValueRecursive(product, "items.0.sellers.0.commertialOffer.AvailableQuantity", Integer.class) > 0;
            Integer price = isAvailable ? JSONUtils.getValueRecursive(product, "items.0.sellers.0.commertialOffer.Price", Integer.class) * 100 : null;

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

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

   }


   private JSONObject getJSONFromAPI() {
      String urlAPI = "https://apis.santaisabel.cl/catalog/api/v1/apoquindo/search/" + this.keywordEncoded + "?page=" + this.currentPage;
      if (urlAPI.contains("+")) {
         urlAPI = urlAPI.replace("+", "%20");
      }
      this.log("Link onde são feitos os crawlers: " + urlAPI);

      Map<String, String> headers = new HashMap<>();
      headers.put("x-api-key", "IuimuMneIKJd3tapno2Ag1c1WcAES97j");
      headers.put("x-consumer", "santaisabel");
      headers.put("authority", "apis.santaisabel.cl");
      headers.put("x-account", account);
      String payload = "{\"selectedFacets\":[{\"key\":\"trade-policy\",\"value\":\"1\"}],\"orderBy\":\"OrderByScoreDESC\"}";

      Request request = Request.RequestBuilder.create()
         .setUrl(urlAPI)
         .setCookies(cookies)
         .setHeaders(headers)
         .setPayload(payload)
         .setFollowRedirects(false)
         .setProxyservice(
            Arrays.asList(
               ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY,
               ProxyCollection.BUY_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_BR
            )
         ).build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new JsoupDataFetcher(), this.dataFetcher), session, "post");
      return CrawlerUtils.stringToJson(response.getBody());

   }

   protected void setTotalProducts(JSONObject searchedJson) {
      this.totalProducts = searchedJson.optInt("recordsFiltered");
      this.log("Total da busca: " + this.totalProducts);

   }

}
