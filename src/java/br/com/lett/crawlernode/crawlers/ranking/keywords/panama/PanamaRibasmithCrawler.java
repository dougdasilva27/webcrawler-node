package br.com.lett.crawlernode.crawlers.ranking.keywords.panama;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONObject;
import org.yaml.snakeyaml.util.UriEncoder;

import java.io.UnsupportedEncodingException;

public class PanamaRibasmithCrawler extends CrawlerRankingKeywords {


   public PanamaRibasmithCrawler(Session session) {
      super(session);
   }


   private JSONObject searchJson() {
      Request request = Request.RequestBuilder.create()
         .setUrl("https://api.fastsimon.com/full_text_search?q=" + keywordEncoded + "&page_num=" + currentPage + "&UUID=19f6f39a-57d5-470f-8795-84369d66b79e")
         .build();

      String response = dataFetcher.get(session, request).getBody();

      return CrawlerUtils.stringToJson(response);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 20;

      JSONObject searchResult = searchJson();

      if (searchResult != null && searchResult.has("items")) {
         if (this.totalProducts == 0) {
            this.totalProducts = searchResult.optInt("total_results");
         }

         for (Object object : searchResult.optJSONArray("items")) {
            JSONObject product = (JSONObject) object;

            String internalId = product.optString("sku");
            String internalPid = product.optString("id");
            String url = scrapUrl(product);
            String name = product.optString("l");
            String imgUrl = product.optString("t");
            Integer price = scrapPrice(product);
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(url)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setImageUrl(imgUrl)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);
         }
      } else {
         log("keyword sem resultado");
      }
   }

   private String scrapUrl(JSONObject product) {
      String url = product.optString("u");
      if (!url.isEmpty()) {
         url = UriEncoder.encode(url);
      }

      return url;
   }

   private int scrapPrice(JSONObject product) {
      String price = product.optString("p");
      return CommonMethods.stringPriceToIntegerPrice(price, '.', null);
   }

   @Override
   protected boolean hasNextPage() {
      return !(this.position >= this.totalProducts);
   }
}
