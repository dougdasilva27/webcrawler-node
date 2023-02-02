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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FalabellaCrawler extends CrawlerRankingKeywords {

   private final String HOME_PAGE = getHomePage();
   private final boolean allow3pSeller = isAllow3pSeller();

   private String categoryUrl = null;

   public FalabellaCrawler(Session session) {
      super(session);
   }

   protected String getHomePage() {
      return session.getOptions().optString("home_page");
   }

   protected boolean isAllow3pSeller() {
      return session.getOptions().optBoolean("allow_3p_seller", true);
   }

   @Override
   protected Document fetchDocument(String url) {
      Map<String, String> head = new HashMap<>();
      String headerCookieString = "userSelectedZone=userselected;IS_ZONE_SELECTED=true;isPoliticalIdExists=true;";
      String localeOptions = session.getOptions().optString("localeOptions");
      if (localeOptions != null && !localeOptions.isEmpty()) {
         head.put("cookie", headerCookieString + localeOptions);
      }
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(head)
         .setFollowRedirects(true)
         .build();
      Response response = dataFetcher.get(session, request);
      String category = response.getRedirectUrl();
      //this is necessary because when has category, we need catch from redirection, but cannot have redirection if the store not allowed 3P.
      if (category != null && category.contains("category")) {
         this.categoryUrl = this.categoryUrl == null ? response.getRedirectUrl() : this.categoryUrl;
      } else {
         request.setFollowRedirects(false);
         response = dataFetcher.get(session, request);
      }


      return Jsoup.parse(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 48;
      String url = " ";
      this.log("Página " + this.currentPage);

      if (allow3pSeller) {
         if (this.categoryUrl != null) {
            url = this.categoryUrl + "?page=" + this.currentPage;
         } else {
            url = HOME_PAGE + "/search?Ntt=" + this.keywordEncoded + "&page=" + this.currentPage;
         }
      } else {
         String storeName = getStoreName(HOME_PAGE);
         if (this.categoryUrl != null) {
            url = this.categoryUrl + "?subdomain=" + storeName + "&page=" + this.currentPage + "&store=" + storeName;
         } else {
            url = HOME_PAGE + "/search?Ntt=" + this.keywordEncoded + "&subdomain=" + storeName + "&page=" + this.currentPage + "&store=" + storeName;
         }
      }

      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);
      JSONObject jsonObject = CrawlerUtils.selectJsonFromHtml(this.currentDoc, "#__NEXT_DATA__", null, null, false, false);
      JSONArray products = jsonObject != null ? JSONUtils.getValueRecursive(jsonObject, "props.pageProps.results", JSONArray.class) : null;

      if (products != null && !products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts(jsonObject);
         }
         for (Object obj : products) {
            if (obj instanceof JSONObject) {
               JSONObject product = (JSONObject) obj;
               String internalId = product.optString("skuId");
               String productUrl = scrapUrl(product, internalId);
               String name = product.optString("displayName");
               String imageUrl = JSONUtils.getValueRecursive(product, "mediaUrls.0", String.class);
               Integer price = scrapPrice(product);
               boolean isAvailable = price != null;

               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(internalId)
                  .setInternalPid(internalId)
                  .setName(name)
                  .setPriceInCents(price)
                  .setAvailability(isAvailable)
                  .setImageUrl(imageUrl)
                  .build();

               saveDataProduct(productRanking);

               if (this.arrayProducts.size() == productsLimit) {
                  break;
               }

            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private String scrapUrl(JSONObject product, String internalId) {
      String productUrl = product.optString("url");
      if (productUrl != null) {
         productUrl = productUrl.replace("?", "/" + internalId + "?");
      }

      return productUrl;
   }

   private String getStoreName(String homePage) {
      String regex = "/([a-z]*)-";

      Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      Matcher matcher = pattern.matcher(homePage);
      if (matcher.find()) {
         return matcher.group(1);
      }
      return null;
   }


   protected void setTotalProducts(JSONObject product) {
      this.totalProducts = JSONUtils.getValueRecursive(product, "props.pageProps.pagination.count", Integer.class);
      this.log("Total da busca: " + this.totalProducts);
   }


   private Integer scrapPrice(JSONObject product) {
      Integer price = null;
      // price is in pesos, like: 1,500, 749 and 1.649.900
      String priceStr = JSONUtils.getValueRecursive(product, "prices.0.price.0", String.class);
      if (priceStr != null) {
         if (priceStr.contains(",") || !priceStr.contains(".")) {
            price = CommonMethods.stringPriceToIntegerPrice(priceStr, '.', null);
         } else {
            price = Integer.valueOf(priceStr.replaceAll("[^0-9]", "").trim());

         }
      }

      return price;
   }

}
