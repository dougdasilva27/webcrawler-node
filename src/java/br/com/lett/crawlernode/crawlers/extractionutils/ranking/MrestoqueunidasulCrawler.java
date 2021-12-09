package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

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
import org.openqa.selenium.WebElement;

public class MrestoqueunidasulCrawler extends CrawlerRankingKeywords {
   Integer products = 0;

   public MrestoqueunidasulCrawler(Session session) {
      super(session);
   }

   protected Document fetch() {
      Document doc = null;

      try {
         String login = session.getOptions().optString("login");
         String password = session.getOptions().optString("password");

         webdriver = DynamicDataFetcher
            .fetchPageWebdriver("https://www.mrestoque.com.br/cliente/entrar", ProxyCollection.BUY_HAPROXY, session);
         webdriver.waitLoad(5000);

         WebElement email = this.webdriver.driver.findElement(By.cssSelector("#username"));
         email.sendKeys(login);
         webdriver.waitLoad(2000);

         WebElement pass = this.webdriver.driver.findElement(By.cssSelector("#password"));
         pass.sendKeys(password);
         webdriver.waitLoad(2000);

         WebElement loginButton = this.webdriver.driver.findElement(By.cssSelector("[data-cy=\"enter-login\"]"));
         webdriver.clickOnElementViaJavascript(loginButton);
         webdriver.waitLoad(5000);

         String url = "https://www.mrestoque.com.br/busca?s=" + this.keywordEncoded + "&order=relevance&pagina=" + this.currentPage;

         webdriver.loadUrl(url);
         this.webdriver.waitLoad(5000);
         doc = Jsoup.parse(webdriver.getCurrentPageSource());
         this.webdriver.waitLoad(5000);
         this.products = scrapTotalProducts(doc);
         webdriver.terminate();
      } catch (Exception e) {
         Logging.printLogDebug(logger, session, CommonMethods.getStackTrace(e));
      }

      return doc;
   }


   @Override
   public void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 24;
      this.log("Página " + this.currentPage);

      this.currentDoc = fetch();

      if (this.currentDoc != null && this.currentDoc.selectFirst("li.align-items--stretch") != null) {
         Elements products = this.currentDoc.select(".products__list li");

         if (!products.isEmpty()) {

            for (Element e : products) {
               String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-image i", "id");
               String urlProduct = CrawlerUtils.scrapUrl(e, ".product-image a", "href", "https", "www.mrestoque.com.br");
               String name = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-image a", "title");
               Integer price = scrapPrice(e);
               String imgUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-image a img", "src");
               Boolean isAvailable = (price != null && price != 0);

               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(urlProduct)
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
         }
      } else {
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

   }

   private Integer scrapTotalProducts(Document doc) {
      Integer totalProducts = CrawlerUtils.scrapIntegerFromHtml(doc, ".result-title em", false, 0);

      return totalProducts;
   }

   private Integer scrapPrice(Element e) {
      String price = CrawlerUtils.scrapStringSimpleInfo(e, "span.new-price", false);
      Integer priceInCents = 0;

      if (price != null) {
         price = price.replaceAll("[^0-9-,]", "");
         Double priceDouble = Double.parseDouble(price.replace(",", "."));
         priceInCents = (int) Math.round((priceDouble * 100));
      }

      return priceInCents;
   }

   @Override
   protected boolean hasNextPage() {
      if (this.products != null && this.totalProducts < this.products) {
         return true;
      }
      return false;
   }
}
