package br.com.lett.crawlernode.crawlers.ranking.keywords.itauna;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItaunaRenaemcasaCrawler extends CrawlerRankingKeywords {

   private int LAST_PRODUCT_INDEX = 0;

   public ItaunaRenaemcasaCrawler(Session session) {
      super(session);
   }

   private Document fetchNextPage() {
      Logging.printLogDebug(logger, session, "fetching next page...");
      WebElement button = webdriver.driver.findElement(By.cssSelector("button.loja-btn-cor-secundaria"));
      webdriver.clickOnElementViaJavascript(button);
      webdriver.waitLoad(8000);

      return Jsoup.parse(webdriver.getCurrentPageSource());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.log("Página " + this.currentPage);

      String url = "https://www.renaemcasa.com.br/?busca=" + this.keywordEncoded;

      if (LAST_PRODUCT_INDEX == 0) {
         this.currentDoc = fetchDocumentWithWebDriver(url, 20000, ProxyCollection.BUY_HAPROXY);
      } else {
         this.currentDoc = fetchNextPage();
      }

      Elements products = this.currentDoc.select("div.product-item-wrapper");

      if (products != null && !products.isEmpty()) {
         for (int i = LAST_PRODUCT_INDEX; i < products.size(); i++) {
            Element product = products.get(i);
            String productUrl = scrapUrl();
            String internalId = scrapInternalId(productUrl);
            String internalPid = CrawlerUtils.scrapStringSimpleInfo(product, ".product-cdg span", true);
            String name = CrawlerUtils.scrapStringSimpleInfo(product, "div.descricao", true);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(product, ".product-img img", Arrays.asList("src"), "https", "cdn.regexsolutions.com.br");

            Integer price = scrapPrice(product);
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalPid(internalPid)
               .setInternalId(internalId)
               .setName(name)
               .setImageUrl(imgUrl)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);

            LAST_PRODUCT_INDEX++;

            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }
   }

   @Override
   protected boolean hasNextPage() {
      return this.currentDoc.selectFirst("button.loja-btn-cor-secundaria") != null;
   }

   private Integer scrapPrice(Element product) {
      Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(product, "div.product-price-car", null, false, ',', session, null);

      if (price == null) {
         price = CrawlerUtils.scrapPriceInCentsFromHtml(product, "div.product-price-promocao", null, false, ',', session, null);
      }

      return price;
   }

   private String scrapUrl() {
      Integer imgCount = LAST_PRODUCT_INDEX + 2;

      WebElement openModal = this.webdriver.driver.findElement(
         By.cssSelector("div.product-item-wrapper:nth-child(n + " + imgCount + ") .product-img img ")
      );

      this.webdriver.clickOnElementViaJavascript(openModal);
      this.webdriver.waitLoad(2000);

      return this.webdriver.getCurURL();
   }

   private String scrapInternalId(String productUrl) {
      String regex = "sku=([0-9]*)";

      if (productUrl != null) {
         Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
         Matcher matcher = pattern.matcher(productUrl);
         if (matcher.find()) {
            return matcher.group(1);
         }
      }

      return null;
   }
}
