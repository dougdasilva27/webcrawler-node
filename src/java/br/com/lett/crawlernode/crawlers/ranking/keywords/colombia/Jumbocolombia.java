package br.com.lett.crawlernode.crawlers.ranking.keywords.colombia;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.kafka.common.protocol.types.Field;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;


public class Jumbocolombia extends CrawlerRankingKeywords {
   private final String Locate_code = session.getOptions().optString("code_locale", "");

   public Jumbocolombia(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      JSONObject apiJson = fetchProducts();
      JSONArray products = apiJson.optJSONArray("products");
      if (!products.isEmpty()) {
         if (totalProducts == 0) {
            totalProducts = apiJson.optInt("size");
         }
         for (Object object : products) {
            JSONObject product = (JSONObject) object;
            StringBuilder url = new StringBuilder();
            url.append("https:");

            String internalId = product.optString("id");
            url.append(product.optString("url"));

            String name = product.optString("name");
            String imgUrl = "https:" + getImage(product);
            Integer price = product.optInt("price");
            boolean  isAvailable  =  product.optString("status").equals("AVAILABLE") ? true : false;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(url.toString())
               .setInternalId(internalId)
               .setName(name)
               .setImageUrl(imgUrl)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);


         }


      } else {
         this.log("keyword sem resultado");
         result = false;
      }


   }

   private String getImage(JSONObject product){
      return  product.optJSONObject("images").optString("default");
   }

   private JSONObject fetchProducts() {
      String url = "https://api.linximpulse.com/engage/search/v3/search?apiKey=tiendasjumbofood&page=" + this.currentPage + "&resultsPerPage=32&terms=" + this.keywordEncoded + "&sortBy=relevance&salesChannel=" + Locate_code;
      //https://api.linximpulse.com/engage/search/v3/search?apiKey=tiendasjumbofood&page=1&resultsPerPage=32&terms=leche&sortBy=relevance&salesChannel=19

      Map<String,String> headers = new HashMap<>();
      headers.put("origin","https://www.tiendasjumbo.co");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build();

      return CrawlerUtils.stringToJson(dataFetcher.get(session,request).getBody());
   }
}
