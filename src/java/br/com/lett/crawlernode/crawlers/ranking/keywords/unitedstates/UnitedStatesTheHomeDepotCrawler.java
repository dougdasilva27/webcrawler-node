package br.com.lett.crawlernode.crawlers.ranking.keywords.unitedstates;

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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import java.util.List;

public class UnitedStatesTheHomeDepotCrawler extends CrawlerRankingKeywords {
   private static final String HOME_PAGE = "https://www.homedepot.com/";
   protected String localizer = getStoreLocalizer();

   public UnitedStatesTheHomeDepotCrawler(Session session) {
      super(session);
   }

   protected String getStoreLocalizer() {
      return session.getOptions().optString("localizer");
   }

   @Override
   protected void processBeforeFetch() {
      Cookie cookie = new Cookie.Builder("THD_LOCALIZER", this.localizer)
         .domain(".homedepot.com")
         .path("/")
         .isHttpOnly(false)
         .isSecure(false)
         .build();
      this.cookiesWD.add(cookie);
   }

   public void scrollDownPage() {
      JavascriptExecutor js = (JavascriptExecutor) webdriver.driver;
      js.executeScript("window.scrollTo(0, document.body.scrollHeight);", "");
      webdriver.waitLoad(7000);
   }

   @Override
   protected Document fetchDocument(String url) {
      Document doc = null;

      try {
         if (this.currentPage == 1) {
            webdriver = DynamicDataFetcher.fetchPageWebdriver(HOME_PAGE, ProxyCollection.NETNUT_RESIDENTIAL_US_HAPROXY, session, this.cookiesWD, this.HOME_PAGE);

            webdriver.waitForElement("input.SearchBox__input", 120);
            webdriver.sendToInput("input.SearchBox__input", this.keywordWithoutAccents, 120);
            webdriver.findAndClick("#headerSearchButton", 120);
            webdriver.waitLoad(70000);
            scrollDownPage();
         } else {
            WebElement nextButton = webdriver.driver.findElement(
               By.cssSelector("a[aria-label=\"Next\"]"));

            webdriver.clickOnElementViaJavascript(nextButton);
            webdriver.waitLoad(70000);
            scrollDownPage();
         }
         doc = Jsoup.parse(webdriver.getCurrentPageSource());

      } catch (Exception e) {
         Logging.printLogInfo(logger, session, CommonMethods.getStackTrace(e));
         webdriver.terminate();
      }
      return doc;
   }


   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {

      this.currentDoc = fetchDocument(this.keywordEncoded);

      Elements results = scrapAllElements(this.currentDoc);

      if (results != null && !results.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }
         if (currentPage == 1) {
            String productCount = CrawlerUtils.scrapStringSimpleInfo(this.currentDoc, "span.results-applied__label", false);
            productCount = productCount.replaceAll("[\\p{Punct}a-zA-Z]", "");
            productCount = productCount.trim();
            this.totalProducts = Integer.parseInt(productCount);
         }
         int i = 0;

         for (Element prod : results) {
            i++;
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(prod, "[data-prop=\"productID\"]", "content");
            String name = CrawlerUtils.scrapStringSimpleInfo(prod, "span.product-pod__title__product", false);
            String productUrl = scrapProductUrl(prod);
            String imageUrl = CrawlerUtils.scrapSimplePrimaryImage(prod, ".product-pod__image-wrapper img", List.of("src"), "https", "");
            int price = CrawlerUtils.scrapPriceInCentsFromHtml(prod, ".price-format__main-price span:nth-child(2)", null, true, '.', session, 0);
            boolean isAvailable = price != 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalPid)
               .setName(name)
               .setImageUrl(imageUrl)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);
            if (this.arrayProducts.size() == productsLimit)
               break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private String scrapProductUrl(Element prod) {
      String url = CrawlerUtils.scrapStringSimpleInfoByAttribute(prod, "a.header.product-pod--ie-fix", "href");

      if (url == null) {
         return null;
      }

      return "https://www.homedepot.com" + url;
   }

   protected Elements scrapAllElements (Document doc) {
      Elements firstPartElements = doc.select("#browse-search-pods-1 .browse-search__pod");
      Elements secondPartElements = doc.select("#browse-search-pods-2 .browse-search__pod");

      if (secondPartElements != null && !secondPartElements.isEmpty()) {
         for (Element product: secondPartElements) {
            firstPartElements.add(product);
         }
      }

      return firstPartElements;
   }
   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".b-sss_tabs-nav_numbers", true, 0);
   }
}
