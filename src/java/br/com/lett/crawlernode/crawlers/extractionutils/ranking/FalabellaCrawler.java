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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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
      this.categoryUrl = this.categoryUrl == null ? response.getRedirectUrl() : this.categoryUrl;

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

      Elements products = this.currentDoc.select(".search-results--products > div");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }
         for (Element e : products) {

            String internalId = scrapInternalId(e);
            String productUrl = crawlProductUrl(e);
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".pod-details span b", false);
            String imageUrl = crawlProductImageUrl(e);
            Integer price = scrapPrice(e);
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
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
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

   protected String scrapInternalId(Element e) {
      String value = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "div[id*=testId-pod-]", "id");
      return CommonMethods.getLast(value.split("-"));
   }

   @Override
   protected void setTotalProducts() {
      String result = CrawlerUtils.scrapStringSimpleInfoByAttribute(this.currentDoc, "#search_numResults", "data-results");

      this.totalProducts = result != null ? Integer.parseInt(result) : 0;
      this.log("Total da busca: " + this.totalProducts);
   }


   private Integer scrapPrice(Element e) {
      Integer price = null;
      // price is in pesos, like: 1,500, 749 and 1.649.900
      String priceStr = CrawlerUtils.scrapStringSimpleInfo(e, ".cmr-icon-container span", true);
      if (priceStr != null) {
         if (priceStr.contains(",") || !priceStr.contains(".")) {
            price = CommonMethods.stringPriceToIntegerPrice(priceStr, '.', null);
         } else {
            price = Integer.valueOf(priceStr.replaceAll("[^0-9]", "").trim());

         }
      }

      return price;
   }

   private String crawlProductUrl(Element e) {
      String url = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".pod-head a", "href");
      if (url == null) {
         url = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".section-head a", "href");
      }
      return url;
   }

   private String crawlProductImageUrl(Element e) {
      String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".pod-head div > a img", "src");
      if (imageUrl == null) {
         imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".section-head a", "href");
      }
      return imageUrl;
   }

}
