package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
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
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BrasilAtacadaomarketplaceCrawler extends CrawlerRankingKeywords {

   private String getRegionsIds() {
      return session.getOptions().optString("regionsIds");
   }

   private String getUserType() {
      return session.getOptions().optString("cb_user_type");
   }

   private String getUserCityId() {
      return session.getOptions().optString("cb_user_city_id");
   }

   private String getStore() {
      return session.getOptions().optString("store");
   }

   public BrasilAtacadaomarketplaceCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   private JSONObject fetchProducts(String url) {
      BasicClientCookie cookie = new BasicClientCookie("cb_user_type", getUserType());
      cookie.setDomain("www.atacadao.com.br");
      cookie.setPath("/");
      this.cookies.add(cookie);

      BasicClientCookie cookie2 = new BasicClientCookie("cb_user_city_id", getUserCityId());
      cookie.setDomain("www.atacadao.com.br");
      cookie.setPath("/");
      this.cookies.add(cookie2);

      Map<String, String> headers = new HashMap<>();
      headers.put("Accept", "*/*");
      headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.0.0 Safari/537.36");


      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setProxyservice(
            List.of(
               ProxyCollection.BUY_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
               ProxyCollection.SMART_PROXY_MX_HAPROXY
            )
         ).setHeaders(headers)
         .setCookies(cookies)
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(this.dataFetcher, new ApacheDataFetcher(), new FetcherDataFetcher(), new JsoupDataFetcher()), session, "get");

      return CrawlerUtils.stringToJson(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      pageSize = 20;
      String url = "https://algolia.cotabest.com.br/catalogo?displayFacets=true&queryString=" + this.keywordWithoutAccents.replace(" ", "%20") + "&commaSeparatedRegionIds=" + this.getRegionsIds() + "&page=" + (this.currentPage - 1) + "&order_by=-relevance";
      JSONObject data = fetchProducts(url);
      JSONArray products = JSONUtils.getJSONArrayValue(data, "results");
      if (products != null && !products.isEmpty()) {
         for (Object e : products) {
            JSONObject product = (JSONObject) e;
            String internalId = product.optString("id", null);
            String productUrl = CrawlerUtils.completeUrl(product.optString("slug"), "https", "www.atacadao.com.br");
            String name = product.optString("name");
            String imageUrl = CrawlerUtils.completeUrl(product.optString("photo"), "https", "www.atacadao.com.br");
            Integer price = getPrice(product);
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setName(name)
               .setImageUrl(imageUrl)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
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

   private Integer getPrice(JSONObject product) {
      Double price;
      JSONArray providers = JSONUtils.getJSONArrayValue(product, "providers");
      if (providers != null) {
         for (Object e : providers) {
            JSONObject provider = (JSONObject) e;
            if (provider != null && !provider.isEmpty()) {
               String storeName = provider.optString("name");
               if (storeName != null && !storeName.isEmpty() && storeName.equalsIgnoreCase(getStore())) {
                  JSONArray prices = JSONUtils.getJSONArrayValue(provider, "prices");
                  if (prices != null && !prices.isEmpty()) {
                     JSONObject objPrice = (JSONObject) prices.opt(0);
                     price = objPrice.optDouble("price");
                     return CommonMethods.doublePriceToIntegerPrice(price, 0);
                  }
               }
            }
         }
      }
      return null;
   }

   @Override
   protected boolean hasNextPage() {
      return true;
   }

}
