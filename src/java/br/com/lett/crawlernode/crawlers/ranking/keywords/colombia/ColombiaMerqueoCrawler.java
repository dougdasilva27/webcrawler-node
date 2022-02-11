package br.com.lett.crawlernode.crawlers.ranking.keywords.colombia;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ColombiaMerqueoCrawler extends CrawlerRankingKeywords {

   public ColombiaMerqueoCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.log("Página " + this.currentPage);

      String url = "https://merqueo.com/api/3.1/stores/63/search?q=+" + this.keywordEncoded + "&page=" + this.currentPage + "&per_page=50";

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
               int price = crawlPrice(attributes);
               boolean available = attributes.optInt("quantity") > 0;

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

   private int crawlPrice(JSONObject product) {
      int price = 0;
      if (product.has("special_price")) {
         price = product.optInt("special_price") * 100;
      } else {
         price = product.optInt("price") * 100;
      }
      return price;
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
               .concat("https://merqueo.com/")
               .concat(attributes.optString("city"))
               .concat("/")
               .concat(attributes.optString("department"))
               .concat("/")
               .concat(attributes.optString("shelf"))
               .concat("/")
               .concat(attributes.optString("product"));
      }


      return productUrl;
   }

   private JSONObject fetchApiProducts(String url) {
      Request request = RequestBuilder
         .create()
         .setUrl(url)
         .mustSendContentEncoding(false)
         .build();

      return CrawlerUtils.stringToJson(new FetcherDataFetcher().get(session, request).getBody());
   }

}
