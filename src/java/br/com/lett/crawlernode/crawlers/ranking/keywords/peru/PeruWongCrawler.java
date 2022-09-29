package br.com.lett.crawlernode.crawlers.ranking.keywords.peru;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.VtexRankingKeywordsNew;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;

public class PeruWongCrawler extends VtexRankingKeywordsNew {

   private static final String HOME_PAGE = "https://www.wong.pe";
   private String storeSaleChannel = session.getOptions().optString("store-sale-channel");
   private String storeName = session.getOptions().optString("store-name");
   public PeruWongCrawler(Session session) {
      super(session);
   }

   @Override
   protected String setHomePage() {return HOME_PAGE;}

   @Override
   protected JSONArray fetchPage(String url) {
      BasicClientCookie cookie = new BasicClientCookie("store-sale-channel", storeSaleChannel);
      cookie.setPath("/");
      cookie.setDomain(".www.wong.pe");
      cookies.add(cookie);

      BasicClientCookie cookie2 = new BasicClientCookie("store-name", storeName);
      cookie2.setPath("/");
      cookie2.setDomain(".www.wong.pe");
      cookies.add(cookie2);

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .setProxyservice(Arrays.asList(ProxyCollection.NETNUT_RESIDENTIAL_MX,ProxyCollection.BONANZA,ProxyCollection.BUY))
         .build();

      Response response = dataFetcher.get(session, request);

      return CrawlerUtils.stringToJsonArray(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      String url = HOME_PAGE + "/api/catalog_system/pub/products/search/" + keywordEncoded.replace("+", "%20")+"?&" + storeSaleChannel+ "&_from=" + ((currentPage - 1) * pageSize) +
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
}
