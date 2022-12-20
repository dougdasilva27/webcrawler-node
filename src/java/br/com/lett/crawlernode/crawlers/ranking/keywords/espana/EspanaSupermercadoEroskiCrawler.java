package br.com.lett.crawlernode.crawlers.ranking.keywords.espana;

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
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EspanaSupermercadoEroskiCrawler extends CrawlerRankingKeywords {

   public EspanaSupermercadoEroskiCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 18;
      this.log("Página " + this.currentPage);
      String url = "https://supermercado.eroski.es/en/search/results/?q=" + this.keywordEncoded + "&suggestionsFilter=false";
      this.log("URL : " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".product-item-lineal");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) setTotalProducts();
         for (Element e : products) {
            String internalPid = null;
            String productUrl = CrawlerUtils.scrapUrl(e, ".product-item-lineal .product-item a", "href", "https", "supermercado.eroski.es/");
            String internalId = getIdByRegex(productUrl);
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".product-title.product-title-resp a", true);
            String image = null;
            boolean available = false;
            Integer priceInCents = null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setImageUrl(image)
               .setName(name)
               .setPriceInCents(priceInCents)
               .setAvailability(available)
               .build();

            saveDataProduct(productRanking);
         }
      }
      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private String getIdByRegex(String productUrl) {
      String id = "123";
      if (productUrl != null) {
         Pattern pattern = Pattern.compile("productdetail\\/([0-9]*)-");
         Matcher matcher = pattern.matcher(productUrl);
         if (matcher.find()) {
            id = matcher.group(1);
         }
      }
      return id;
   }


   @Override
   protected boolean hasNextPage() {
      return !this.currentDoc.select(".right .pagination .next").isEmpty();
   }


   public static void waitForElement(WebDriver driver, String cssSelector) {
      WebDriverWait wait = new WebDriverWait(driver, 10);
      wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(cssSelector)));
   }

   @Override
   protected Document fetchDocument(String url) {
      Document doc = null;

      int attempts = 0;

      List<String> proxies = List.of(ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY, ProxyCollection.LUMINATI_SERVER_BR_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_ANY_HAPROXY);
      do {

         try {
            webdriver = DynamicDataFetcher.fetchPageWebdriver(url, proxies.get(attempts), session);

            webdriver.waitLoad(30000);
            doc = Jsoup.parse(webdriver.getCurrentPageSource());

         } catch (Exception e) {
            Logging.printLogInfo(logger, session, CommonMethods.getStackTrace(e));

         } finally {
            if (webdriver != null) {
               webdriver.terminate();
            }
         }
      } while (doc == null && attempts++ < 3);

      return doc;
   }
}
