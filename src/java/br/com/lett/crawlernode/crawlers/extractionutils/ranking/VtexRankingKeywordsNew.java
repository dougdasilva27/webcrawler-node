package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public abstract class VtexRankingKeywordsNew extends CrawlerRankingKeywords {

   protected abstract String setHomePage();

   private final String homePage = setHomePage();

   protected VtexRankingKeywordsNew(Session session) {
      super(session);
      this.pageSize = 12;
   }

   @Override
   protected void extractProductsFromCurrentPage() {

      String url = homePage + "/api/catalog_system/pub/products/search/" + keywordEncoded.replace("+", "%20") + "?_from=" + ((currentPage - 1) * pageSize) +
         "&_to=" + ((currentPage) * pageSize);

      JSONArray products = fetchPage(url);

      for (Object object : products) {

         JSONObject product = (JSONObject) object;
         String productUrl = product.optString("link");
         String internalPid = product.optString("productId");
         String name = product.optString("productName");

         JSONObject itemData = (JSONObject) product.optQuery("/items/0");
         String image = null;
         int priceInCents = 0;
         boolean isAvailable = false;

         if(itemData != null) {
            JSONArray images = itemData.optJSONArray("images");
            JSONArray sellers = itemData.optJSONArray("sellers");
            image = crawlImage(images);

            if(sellers != null && sellers.length() > 0) {
               JSONObject seller = sellers.optJSONObject(0);
               JSONObject commertialOffer = seller.optJSONObject("commertialOffer");

               priceInCents = crawlPrice(commertialOffer);
               isAvailable = commertialOffer.optBoolean("IsAvailable");
            }
         }

         try {
            RankingProduct rankingProduct = RankingProductBuilder.create()
               .setInternalPid(internalPid)
               .setUrl(productUrl)
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
   }

   protected int crawlPrice(JSONObject commertialOffer) {
      int priceInCents = 0;

         if(commertialOffer != null) {
            Object price = commertialOffer.opt("Price");
            if (price instanceof Double) {
               priceInCents = (int) Math.round((Double) price * 100);
            } else if (price instanceof Integer) {
               priceInCents = (int) price * 100;
            }
         }
      return priceInCents;
   }

   protected String crawlImage(JSONArray images) {
         if (images != null && images.length() > 0) {
            JSONObject image = images.optJSONObject(0);
            if(image != null) {
               return image.optString("imageUrl");
            }
         }
      return null;
   }

   protected JSONArray fetchPage(String url) {

      Map<String, String> headers = new HashMap<>();
      if (cookies != null && !cookies.isEmpty()) {
         headers.put("cookie", CommonMethods.cookiesToString(cookies));
      }

      Request request = Request.RequestBuilder.create()
         .setHeaders(headers)
         .setUrl(url)
         .build();

      Response response = dataFetcher.get(session, request);

      return CrawlerUtils.stringToJsonArray(response.getBody());
   }

   @Override
   protected boolean hasNextPage() {
      return ((arrayProducts.size() - 1) % pageSize - currentPage) < 0;
   }

}
