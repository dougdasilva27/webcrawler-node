package br.com.lett.crawlernode.crawlers.ranking.keywords.riodejaneiro;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class RiodejaneiroZonasulCrawler extends CrawlerRankingKeywords {

   private String categoryUrl;

   public RiodejaneiroZonasulCrawler(Session session) {
      super(session);
   }

   protected String getHomePage() {
      return session.getOptions().getString("home_page");
   }

   protected String getVtexSegment() {
      return session.getOptions().getString("vtex_segment");
   }

   @Override
   protected void processBeforeFetch() {
      BasicClientCookie userLocationData = new BasicClientCookie("vtex_segment", getVtexSegment());
      userLocationData.setPath("/");
      cookies.add(userLocationData);
   }

   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.log("Página " + this.currentPage);
      this.pageSize = 48;
       String url = "https://www.zonasul.com.br/" + this.keywordEncoded + "?_q=" + this.keywordEncoded + "&map=ft&page=" + this.currentPage;

      if (this.currentPage > 1 && this.categoryUrl != null) {
         url = this.categoryUrl + "?page=" + this.currentPage;
      }

      this.currentDoc = webdriverRequest(url);

      if (this.currentPage == 1) {
         String redirectUrl = CrawlerUtils.getRedirectedUrl(url, session);

         if (!url.equals(redirectUrl)) {
            this.categoryUrl = redirectUrl;
         }
      }

      Elements products = this.currentDoc.select(".vtex-search-result-3-x-galleryItem.vtex-search-result-3-x-galleryItem--small.pa4");
      if (!products.isEmpty()) {

         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Element product : products) {
            String productUrl = CrawlerUtils.completeUrl(CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".vtex-product-summary-2-x-container.vtex-product-summary-2-x-containerNormal a", "href"), "https", "www.zonasul.com.br");
            String internalPid = CommonMethods.getLast(productUrl.split("-")).replace("/p", "");
            String name = CrawlerUtils.scrapStringSimpleInfo(product, ".vtex-product-summary-2-x-productBrand.vtex-product-summary-2-x-brandName.t-body", true);
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".dib.relative.vtex-product-summary-2-x-imageContainer.vtex-product-summary-2-x-imageStackContainer img", "src");
            int price = CrawlerUtils.scrapIntegerFromHtml(product, ".vtex-difference__por", true, 0);
            boolean isAvailable = price != 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(null)
               .setInternalPid(internalPid)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
               .build();

            saveDataProduct(productRanking);

            this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);

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

   protected void setTotalProducts(JSONObject data) {
      this.totalProducts = CrawlerUtils.getIntegerValueFromJSON(data, "total", 0);
      this.log("Total da busca: " + this.totalProducts);
   }

   @Override
   protected boolean hasNextPage() {
      return !this.currentDoc.select(".vtex-button__label.flex.items-center.justify-center.h-100.ph5").isEmpty();
   }


   private Document webdriverRequest(String url) {
      Document doc;

      try {
         webdriver = DynamicDataFetcher.fetchPageWebdriver(url, ProxyCollection.LUMINATI_SERVER_BR_HAPROXY, session);
         if (webdriver != null) {
            waitForElement(webdriver.driver, ".vtex-search-result-3-x-galleryItem.vtex-search-result-3-x-galleryItem--small.pa4");

            doc = Jsoup.parse(webdriver.getCurrentPageSource());
            webdriver.terminate();
         } else {
            throw new WebDriverException("Failed to instantiate webdriver");
         }
      } catch (Exception e) {
         Logging.printLogDebug(logger, session, CommonMethods.getStackTrace(e));
         throw e;
      }

      return doc;
   }

   public static void waitForElement(WebDriver driver, String cssSelector) {
      WebDriverWait wait = new WebDriverWait(driver, 70);
      wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(cssSelector)));
   }

}

