package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.LinxImpulseRanking;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class BrasilCompreAgoraCrawler extends LinxImpulseRanking {
   public BrasilCompreAgoraCrawler(Session session) {
      super(session);
   }

   private Integer price;
   private String internalId;
   private JSONArray pricesJson;

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      String url = mountURL();
      JSONObject data = fetchPage(url);
      JSONArray products = data.optJSONArray("products");



      if (products != null && !products.isEmpty()) {
         String productIds = MountUrlid(products);
         fetchPrice(productIds);
         this.totalProducts = data.optInt("size");
         for (Object object : products) {
            JSONObject product = (JSONObject) object;
            String productUrl = crawlProductUrl(product);
            String internalPid = crawlInternalPid(product);
            internalId = crawlInternalId(product, internalPid);
            String name = product.optString("name");
            String image = crawlImage(product);
            int priceInCents = crawlPrice(product);
            boolean isAvailable = crawlAvailability(product);
            try {
               RankingProduct rankingProduct = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setImageUrl(image)
                  .setPriceInCents(priceInCents)
                  .setAvailability(isAvailable)
                  .build();

               saveDataProduct(rankingProduct);
            } catch (MalformedProductException e) {
               this.log(e.getMessage());
            }
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

   @Override
   protected int crawlPrice(JSONObject product) {
      try {
         for(Object object:  pricesJson){
            JSONObject obj = (JSONObject) object;
            if (internalId.equals(obj.optString("productId"))){
               price = obj.optJSONObject("skus").optInt("pricePerUnit");
               return price;
            }

         }

      } catch (NullPointerException pointer) {
         price = 0;
      }
      price = 0;
      return price;
   }

   @Override
   protected boolean crawlAvailability(JSONObject product) {
      return price != 0;
   }

   protected void fetchPrice(String id) {
      String url = "https://www.compra-agora.com/api/productLookup/" + id;
      Map<String, String> headers = new HashMap<>();
      headers.put("Cookie", "CPL=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJpbmZyYWNvbW1lcmNlLmNvbS5iciIsInN1YiI6IkluZnJhY29tbWVyY2UiLCJhdWQiOiJjb21wcmEtYWdvcmEuY29tIiwiaWF0IjoxNjQ0MjQ5MTY2LCJkYXRhIjp7InVpZCI6IlRvYndGUmdjS3pOckNzZXhuUnozWnk3a3JwNEJ3T2VUTGxpRmtBNzR1czQ9In19.ob8UfpN1WMPTagamtSsoco1cllnNTSRwNeKrbI2Q9oc2HNjUL56VVEUc8vuz_jUtYEQ3RZAJZJLf5vSV_wXT6V9dkrGP2L7BilB3JamV56muHBqOjA3Xhii6qGFQePgAvNoZaCrw75-pIDSwzfqEzXDYoNqxkBQfVvTG-FCKTKtbVZqcTNXAcNjA80dp9AydpkCi712NxGITCnigZVFaktWl3B3NuBFd44oE-qKI5JCp6IwbM0ptgBHjAT8i43AmLsdS_-JCPcBjbBTFgS_27KNQ26dvuuZjdSwzKHYSxvUO2rI7Vz_4TVFHdaNA-MSfPksfzZT_B_HnX7FkC-imoA; PHPSESSID=7hslf6eioumlusjacjolnn8cf1; ccw=2 3 61 94 147; usrfgpt=367359160001371644248354");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build();

      Response response = dataFetcher.get(session, request);

      pricesJson = CrawlerUtils.stringToJsonArray(response.getBody());
   }


   protected String MountUrlid(JSONArray products){
      String url="";
      for (Object object : products) {
         JSONObject product = (JSONObject) object;
         url = url + "+" +crawlInternalPid(product);
      }
      return url;
   }
}
