package br.com.lett.crawlernode.crawlers.ranking.keywords.guatemala;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
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
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {

      JSONObject searchResult = searchJson();

      if (searchResult != null && searchResult.has("Products")) {

         if (this.totalProducts == 0) {
            this.totalProducts = searchResult.optInt("ProductsCount");
         }

         for (Object object : searchResult.optJSONArray("Products")) {
            JSONObject products = (JSONObject) object;
            String internalPid = null;
            String name = null;
            String imgUrl = null;
            Integer price = null;


            for (Object obj : products.optJSONArray("AddtionalFields")) {
               JSONObject info = (JSONObject) obj;
               String nameInfo = info.optString("Name");
               switch (nameInfo) {
                  case "productid":
                     internalPid = info.optString("Value");
                     break;
                  case "Title":
                     name = info.optString("Value");
                     break;
                  case "Image_link":
                     imgUrl = "https://www.cemaco.com/" + info.optString("Value");
                     break;
                  case "Price":
                     String priceString = info.optString("Value");
                     priceString = priceString.replaceAll("\\.", "");
                     price = Integer.parseInt(priceString);

                     break;
               }
            }
            String url = getUrl(products);
            Boolean isAvailable = price>0;
            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(url)
               .setInternalPid(internalPid)
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
