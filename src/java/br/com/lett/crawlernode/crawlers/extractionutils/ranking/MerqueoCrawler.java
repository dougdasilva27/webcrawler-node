package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import com.sun.jdi.IntegerValue;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MerqueoCrawler extends CrawlerRankingKeywords {

   public MerqueoCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.log("Página " + this.currentPage);

      String url = "https://merqueo.com/api/3.1/stores/281/search?q=+" + this.keywordEncoded + "&per_page=" + this.currentPage + "&per_page=50&zoneId=" + session.getOptions().optString("zoneId") + "&sort_by=relevance";


      this.log("Link onde são feitos os crawlers: " + url);

      JSONObject apiJson = fetchApiProducts(url);
      JSONArray productsArray = new JSONArray();

      if (apiJson.has("data") && !apiJson.isNull("data")) {
         productsArray = apiJson.optJSONArray("data");
      }

      if (productsArray.length() > 0) {
         if (this.totalProducts == 0) {
            this.totalProducts = productsArray.length();
         }

         for (Object object : productsArray) {
            JSONObject product = (JSONObject) object;

            if (product.optString("type").equals("products")) {
               String internalId = product.optString("id");
               String productUrl = assembleProductUrl(internalId, apiJson);

               JSONObject attributes = product.optJSONObject("attributes");

               String name = attributes.optString("name");
               String image = attributes.optString("image_large_url");
               boolean available = attributes.optInt("quantity") > 0;
               Integer price = available ? crawlPrice(attributes) : null;

               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(internalId)
                  .setInternalPid(null)
                  .setImageUrl(image)
                  .setName(name)
                  .setPriceInCents(price)
                  .setAvailability(available)
                  .build();

               saveDataProduct(productRanking);

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
      return true;
   }

   private Integer crawlPrice(JSONObject product) {
      double price;
      if (product.has("special_price") && (product.optInt("special_price") != 0)) {
         price = product.optDouble("special_price") * 100;
      } else {
         price = product.optDouble("price") * 100;
      }
      return Math.toIntExact(Math.round(price));
   }

   /*
    * Url exemple:
    * https://merqueo.com/bogota/despensa/condimentos-especias-y-adobos/canela-su-despensa-20-gr
    */
   private String assembleProductUrl(String internalId, JSONObject apiJson) {
      String productUrl = "";

      JSONArray includedArray = apiJson.optJSONArray("included");

      List<JSONObject> jsonObject = IntStream
         .range(0, includedArray.length())
         .mapToObj(includedArray::optJSONObject)
         .filter(json -> json.optString("id").equals(internalId)).collect(Collectors.toList());

      if (jsonObject.isEmpty()) return "";

      JSONObject included = jsonObject.get(0);

      if (included.has("attributes") && !included.isNull("attributes")) {
         JSONObject attributes = included.getJSONObject("attributes");
         productUrl = productUrl
            .concat("https://merqueo.com");
         String country = session.getOptions().optString("country");
         if(country != null && !country.isEmpty()){
            productUrl = productUrl.concat(".").concat(country);

         }
         productUrl = productUrl
            .concat("/")
            .concat(attributes.optString("city"))
            .concat("/");
         String locate = session.getOptions().optString("locate");
            if(locate != null && !locate.isEmpty()){
               productUrl = productUrl .concat(locate)
                  .concat("/");
            }

         productUrl = productUrl
            .concat(attributes.optString("department"))
            .concat("/")
            .concat(attributes.optString("shelf"))
            .concat("/")
            .concat(attributes.optString("product"));
      }


      return productUrl;
   }

   private JSONObject fetchApiProducts(String url) {
      Request request = Request.RequestBuilder
         .create()
         .setUrl(url)
         .setProxyservice(Arrays.asList(ProxyCollection.BUY, ProxyCollection.NETNUT_RESIDENTIAL_BR))
         .mustSendContentEncoding(false)
         .build();

      return CrawlerUtils.stringToJson(CrawlerUtils.retryRequestString(request,List.of(new ApacheDataFetcher(), new JsoupDataFetcher(), new FetcherDataFetcher()),session));
   }

}
