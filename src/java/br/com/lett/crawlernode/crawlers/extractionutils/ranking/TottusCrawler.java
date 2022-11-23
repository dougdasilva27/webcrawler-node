package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

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
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class TottusCrawler extends CrawlerRankingKeywords {

   protected String homePage = "https://www.tottus.com.pe";

   private String channel = session.getOptions().optString("channel", "912");

   public TottusCrawler(Session session) {
      super(session);
   }


   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 48;
      this.log("Página " + this.currentPage);


      String url = homePage + "/api/product-search?q=" + this.keywordEncoded + "&sort=score&" + channel + "&page=" + this.currentPage + "&perPage=48";

      JSONObject jsonInfo = resultFromApi(url);

      JSONArray results = jsonInfo.optJSONArray("results");

      if (results != null && !results.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts(jsonInfo);
         }

         for (Object e : results) {

            JSONObject skuInfo = (JSONObject) e;
            String internalId = skuInfo.optString("id");
            String productUrl = CrawlerUtils.completeUrl(skuInfo.optString("key"), "https", "www.tottus.com.pe") + "/p/";
            String name = skuInfo.optString("name");
            String imgUrl = JSONUtils.getValueRecursive(skuInfo, "images.0", String.class);
            Integer price = scrapPrice(skuInfo);
            boolean isAvailable = price != null;
            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
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
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private Integer scrapPrice(JSONObject skuInfo) {
      JSONObject prices = skuInfo.optJSONObject("prices");
      Double priceRanking;
      Double spotlightPrice = null;
      Double priceFrom = null;
      if (prices != null) {
         spotlightPrice = JSONUtils.getDoubleValueFromJSON(prices, "currentPrice", true);
         priceFrom = JSONUtils.getDoubleValueFromJSON(prices, "regularPrice", true);
      }
      if (spotlightPrice != null) {
         priceRanking = spotlightPrice;
      } else {
         priceRanking = priceFrom;
      }

      return CommonMethods.doublePriceToIntegerPrice(priceRanking, null);

   }

   private JSONObject fetchJsonFromApi(String url) {
      Map<String, String> headers = new HashMap<>();
      if (channel != null) {
         headers.put("cookie", channel);
      }
      Request request = Request.RequestBuilder.create()
         .setHeaders(headers)
         .setUrl(url)
         .setProxyservice(List.of(ProxyCollection.NETNUT_RESIDENTIAL_BR, ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY))
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher(), new FetcherDataFetcher()), session);

      return CrawlerUtils.stringToJson(response.getBody());

   }

   private JSONObject resultFromApi(String url) {
      JSONObject jsonApi = fetchJsonFromApi(url);
      if (jsonApi.has("redirect")) {
         String keywordCategory = getKeywordCategory(jsonApi.optString("redirect", ""));
         String redirectUrl = "https://www.tottus.com.pe/api/product-search/by-category-slug?slug=" + keywordCategory + "&sort=recommended_web&channel=" + channel + "&page=" + this.currentPage + "&perPage=48";
         jsonApi = fetchJsonFromApi(redirectUrl);
         if (jsonApi.optJSONArray("results") != null && jsonApi.optJSONArray("results").length() == 0) {
            redirectUrl = "https://www.tottus.com.pe/api/product-search/by-category-slug?slug=" + keywordCategory + "&sort=recommended_web&channel=" + channel + "_RegularDelivery12&page=" + this.currentPage + "&perPage=48";
            jsonApi = fetchJsonFromApi(redirectUrl);
         }
      }

      return jsonApi;
   }

   private String getKeywordCategory(String redirectUrl) {
      String keywordCategory = "";
      Pattern pattern = Pattern.compile("www.tottus.com.*\\/(.*)\\/c");
      Matcher matcher = pattern.matcher(redirectUrl);
      if (matcher.find()) {
         keywordCategory = matcher.group(1);
      }
      return keywordCategory;
   }


   protected void setTotalProducts(JSONObject jsonInfo) {
      this.totalProducts = JSONUtils.getValueRecursive(jsonInfo, "pagination.totalProducts", Integer.class, 0);
      this.log("Total da busca: " + this.totalProducts);
   }

}
