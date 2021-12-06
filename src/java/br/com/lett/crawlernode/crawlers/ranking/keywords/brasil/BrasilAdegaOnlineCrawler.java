package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.models.*;
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
import org.openqa.selenium.WebElement;

import java.util.*;

public class BrasilAdegaOnlineCrawler extends CrawlerRankingKeywords {
   Integer products;

   public BrasilAdegaOnlineCrawler(Session session) {
      super(session);
   }

   protected Document fetch() {
      String url = "https://www.adegaonline.com.br/search?q=" + this.keywordEncoded + "&type=product&page=" + this.currentPage;
      Document doc = null;

      try {
         webdriver = DynamicDataFetcher.fetchPageWebdriver(url, ProxyCollection.LUMINATI_SERVER_BR_HAPROXY, session);

         webdriver.waitLoad(20000);

         WebElement ageVerificationButton = webdriver.driver.findElement(By.cssSelector("#agp_row > div > div > div.agp__inputContainer > div > form:nth-child(1) > input"));
         webdriver.clickOnElementViaJavascript(ageVerificationButton);

         webdriver.waitForElement(".ProductList--grid div.ProductItem", 30);
         webdriver.waitPageLoad(20);

         doc = Jsoup.parse(webdriver.getCurrentPageSource());

         this.products = scrapTotalProducts(doc);
      } catch (Exception e) {
         Logging.printLogInfo(logger, session, CommonMethods.getStackTrace(e));

      }

      return doc;
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 20;
      this.log("Página " + this.currentPage);

      this.currentDoc = fetch();

      if (this.currentDoc.selectFirst("div.ProductListWrapper") != null) {
         Elements products = this.currentDoc.select(".ProductList--grid div.Grid__Cell");

         if (!products.isEmpty()) {
            for (Element e : products) {

               String name = CrawlerUtils.scrapStringSimpleInfo(e, ".ProductItem__Title", false);
               String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "div.Grid__Cell", "data-id");
               Double price = CrawlerUtils.scrapDoublePriceFromHtml(e, "span.ProductItem__Price", null, true, ',', session);
               Integer priceInCents = (int) Math.round((price * 100));
               Boolean available = (e.select("ProductItem__Label--sold-off").isEmpty()) ? true : false;//arrumar
               String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, "img.ProductItem__Image", Arrays.asList("srcset"), "https", "https://cdn.shopify.com/");
               String productUrl = scrapUrl(e);

               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(internalId)
                  .setName(name)
                  .setImageUrl(imgUrl)
                  .setPriceInCents(priceInCents)
                  .setAvailability(available)
                  .build();

               saveDataProduct(productRanking);
            }
         }
         this.log("Keyword com resultado!");
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }
      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private String scrapUrl(Element e) {
      String urlFullPath = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "a.ProductItem__ImageWrapper", "href");
      String [] urlPath = (urlFullPath != null) ? urlFullPath.split("/") : null;

      if (urlPath != null) {
         return "https://www.adegaonline.com.br/products/" + urlPath[urlPath.length - 1];
      }

      return null;
   }

   private Integer scrapTotalProducts(Element e) {
      String totalProducts = CrawlerUtils.scrapStringSimpleInfo(e, ".boost-pfs-search-result-panel-item > button", false);

      totalProducts = totalProducts.replaceAll("[^0-9]", "");

      return Integer.parseInt(totalProducts);
   }

   @Override
   protected boolean hasNextPage() {
      if (this.products != null && this.totalProducts < this.products) {
         return true;
      }
      return false;
   }
}
