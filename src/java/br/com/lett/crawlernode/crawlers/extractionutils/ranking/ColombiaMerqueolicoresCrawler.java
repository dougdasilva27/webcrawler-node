package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ColombiaMerqueolicoresCrawler extends CrawlerRankingKeywords {

   public ColombiaMerqueolicoresCrawler(Session session) {
      super(session);
   }

   private final String zoneId = session.getOptions().optString("zoneId");
   private final String store = session.getOptions().optString("store");
   private final String store_name = session.getOptions().optString("store_name");

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.log("Página " + this.currentPage);

      String url = "https://merqueo.com/api/3.1/stores/" + store + "/search?q=" + this.keywordEncoded + "&page=" + this.currentPage + "&per_page=50&zoneId=" + zoneId + "&sort_by=relevance";
      this.log("Link onde são feitos os crawlers: " + url);

      JSONObject apiJson = fetchApiProducts(url);
      JSONArray productsArray = new JSONArray();

      if (apiJson.has("included") && !apiJson.isNull("included")) {
         productsArray = apiJson.getJSONArray("data");

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

               if (attributes == null) continue;

               String name = attributes.optString("name");
               String imgUrl = attributes.optString("image_large_url");
               int price = JSONUtils.getPriceInCents(attributes, "price");
               boolean available = attributes.optBoolean("status");

               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(internalId)
                  .setInternalPid(null)
                  .setImageUrl(imgUrl)
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

   private String assembleProductUrl(String internalId, JSONObject data) {
      JSONArray includedArray = data.optJSONArray("included");
      String productUrl = "";

      if (includedArray != null && !includedArray.isEmpty()) {
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
               .concat(store_name)
               .concat("/")
               .concat(attributes.optString("department"))
               .concat("/")
               .concat(attributes.optString("shelf"))
               .concat("/")
               .concat(attributes.optString("product"));
         }
      }

      return productUrl;
   }

   private JSONObject fetchApiProducts(String url) {

      Request request = Request.RequestBuilder
         .create()
         .setUrl(url)
         .mustSendContentEncoding(false)
         .build();

      return CrawlerUtils.stringToJson(new FetcherDataFetcher().get(session, request).getBody());
   }

}
