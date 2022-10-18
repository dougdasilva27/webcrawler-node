package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
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

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class BrasilEsalpetCrawler extends CrawlerRankingKeywords {

   public BrasilEsalpetCrawler(Session session) {
      super(session);
   }

   private Document fetchDocument() {

      Document doc = null;
      int attemp = 0;
      do {
         try {
            Logging.printLogDebug(logger, session, "Fetching page with webdriver...");

            webdriver = DynamicDataFetcher.fetchPageWebdriver("https://www.esalpet.com.br/lista/search/" + this.keywordWithoutAccents, ProxyCollection.LUMINATI_SERVER_BR_HAPROXY, session);
            webdriver.waitLoad(10000);

            doc = Jsoup.parse(webdriver.getCurrentPageSource());

         } catch (Exception e) {
            Logging.printLogDebug(logger, session, CommonMethods.getStackTrace(e));
            Logging.printLogWarn(logger, "Página não capturada");

         } finally {
            if (webdriver != null) {
               webdriver.terminate();
            }
         }
      } while (doc == null && attemp++ < 3);

      return doc;

   }

   public static void waitForElement(WebDriver driver, String cssSelector) {
      WebDriverWait wait = new WebDriverWait(driver, 20);
      wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(cssSelector)));
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {

      Document doc = fetchDocument();
      Elements products = doc != null ? doc.select(".col-6.p-1") : null;
      this.totalProducts = products != null ? products.size() : 0;
      if (products != null && !products.isEmpty()) {
         for (Element product : products) {
            String productUrl = crawlUrl(product);
            String internalId = getId(productUrl);
            String name = CrawlerUtils.scrapStringSimpleInfo(product, ".title.hiddendescricao", true);
            String imageUrl = CrawlerUtils.scrapSimplePrimaryImage(product, ".img-wrap img", Arrays.asList("src"), "", "");
            boolean available = product.select(".alert.alert-light.text-center.div-indisponivel").isEmpty();
            Integer price = available ? CrawlerUtils.scrapPriceInCentsFromHtml(product, ".price-new", null, false, ',', session, null) : null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(available)
               .setImageUrl(imageUrl)
               .build();

            saveDataProduct(productRanking);

            if (this.arrayProducts.size() == productsLimit)
               break;
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");
   }

   private String crawlUrl(Element product) {
      String productUrl = CrawlerUtils.scrapUrl(product, ".item-slide a", Arrays.asList("href"), "https", "");
      if (productUrl != null && !productUrl.isEmpty()) {
         return productUrl.replace("///", "//");
      }

      return null;

   }


   private String getId(String url) {
      String id = null;

      if (url != null) {

         Pattern pattern = Pattern.compile("id=([0-9]+)", Pattern.MULTILINE);
         final Matcher matcher = pattern.matcher(url);

         if (matcher.find()) {
            id = matcher.group(1);

         }
      }

      return id;
   }

}
