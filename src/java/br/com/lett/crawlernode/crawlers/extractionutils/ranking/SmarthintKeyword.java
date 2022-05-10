package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

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

import java.io.UnsupportedEncodingException;

public class SmarthintKeyword extends CrawlerRankingKeywords {

   private final String SH_key = session.getOptions().optString("SH_KEY");
   private final String version = session.getOptions().optString("version", "v5");
   private final String rule = session.getOptions().optString("rule", "");
   private final String searchSort = session.getOptions().optString("searchSort", "0");

   public SmarthintKeyword(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {

      this.pageSize = session.getOptions().optInt("pageSize", 12);

      JSONObject api = fetchApi();

      if (api.has("Products") && !api.optJSONArray("Products").isEmpty()) {
         JSONArray products = api.optJSONArray("Products");
         for (Object o : products) {
            JSONObject product = (JSONObject) o;

            String productUrl = CrawlerUtils.completeUrl(product.optString("Id"), "https", "");
            String internalId = product.optString("ProductId");
            String internalPid = product.optString("ItemGroupId");
            String name = product.optString("Title");
            String imageUrl = scrapImage(product);
            int price = scrapPrice(product);
            boolean isAvailable = product.optString("Availability", "").equalsIgnoreCase("in stock");

            //New way to send products to save data product
            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setImageUrl(imageUrl)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);
         }


      } else {
         log("keyword sem resultado");
         this.result = false;
      }

   }

   private String scrapImage(JSONObject product) {
      String image = product.optString("ImageLink");

      if (!image.isEmpty()) {
         image = CrawlerUtils.completeUrl(image, "https", "supernossoemcasa.com.br");
      }
      return image;
   }

   private int scrapPrice(JSONObject product) {
      int price = JSONUtils.getPriceInCents(product, "SalePrice");

      if (price == 0) {
         price = JSONUtils.getPriceInCents(product, "Price");
      }

      return price;
   }

   @Override
   protected boolean hasNextPage() {
      return true;
   }

   private JSONObject fetchApi() {
      StringBuilder builder = new StringBuilder();
      builder.append("https://search.smarthint.co/")
         .append(version)
         .append("/Search/GetPrimarySearch?")
         .append("shcode=").append(SH_key)
         .append("&term=").append(keywordEncoded.replace("+", "%20"))
         .append("&from=").append((this.currentPage - 1) * this.pageSize)
         .append("&size=").append(this.pageSize)
         .append("&searchSort=").append(searchSort);

      if (!rule.isEmpty()) {
         builder.append("&rule=").append(rule);
      }

      String url = builder.toString();

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .build();

      return CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
   }

}
