package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.crawler.DiscoveryCrawlerSession;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.HashMap;
import java.util.Map;


public abstract class RappiCrawlerRanking extends CrawlerRankingKeywords {

   protected String PRODUCT_BASE_URL = "https://www." + getProductDomain() + "/product/";

   public RappiCrawlerRanking(Session session) {
      super(session);
   }

   protected String getStoreId(){
      return session.getOptions().optString("storeId");
   }

   private String getCurrentLocation() {
      return session.getOptions().optString("currentLocation");
   }

   protected abstract String getApiDomain();

   protected abstract String getProductDomain();

   protected abstract String getMarketBaseUrl();

   protected abstract String getImagePrefix();

   private Document fetch(String url) {
      BasicClientCookie cookie = new BasicClientCookie("currentLocation", getCurrentLocation());
      cookie.setDomain(".www." + getProductDomain());
      cookie.setPath("/");
      cookies.add(cookie);

      Request request = RequestBuilder.create()
         .setCookies(cookies)
         .setUrl(url)
         .setFollowRedirects(true)
         .build();

      Response response = dataFetcher.get(session, request);

      return Jsoup.parse(response.getBody());
   }

   @Override
   public void extractProductsFromCurrentPage() throws MalformedProductException {

      this.log("Página " + this.currentPage);

      String marketUrl = getMarketBaseUrl() + getStoreId();
      this.currentDoc = fetch(marketUrl + "/s?term=" + this.keywordEncoded);


      JSONObject pageJson = CrawlerUtils.selectJsonFromHtml(this.currentDoc, "#__NEXT_DATA__", null, null, false, false);
      JSONArray products = JSONUtils.getValueRecursive(pageJson, "props.pageProps.products", JSONArray.class, new JSONArray());

      if (products.length() > 0) {
         for (int i = 0; i < products.length(); i++) {
            JSONObject product = products.getJSONObject(i);

            String internalId = product.optString("product_id");
            String id = product.optString("id", "");
            String url = !id.equals("") ? PRODUCT_BASE_URL + id : "";
            String name = product.optString("name");
            Integer priceInCents = scrapPrice(product);
            boolean isAvailable = product.optBoolean("in_stock");
            String imageUrl = crawlProductImage(product);

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(url)
               .setInternalId(internalId)
               .setName(name)
               .setPriceInCents(priceInCents)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
               .build();


            saveDataProduct(productRanking);


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

   private String crawlProductImage(JSONObject product) {
      return CrawlerUtils.completeUrl(product.optString("image"), "https", getImagePrefix());
   }


   private Integer scrapPrice(JSONObject product) {
      double price = product.optDouble("price");
      Integer priceInCents = null;
      if (price != 0.0) {
         priceInCents = Integer.parseInt(Double.toString(price).replace(".", ""));
      }
      return priceInCents;
   }

}
