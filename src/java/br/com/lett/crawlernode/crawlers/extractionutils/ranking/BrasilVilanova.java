package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

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
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.print.Doc;
import java.util.List;

public class BrasilVilanova extends CrawlerRankingKeywords {

   public BrasilVilanova(Session session) {
      super(session);
      super.fetchMode = FetchMode.WEBDRIVER;
   }

   private final String HOME_PAGE = "https://www.vilanova.com.br/";

   @Override
   protected void processBeforeFetch() {
      Cookie cookie = new Cookie.Builder("modoGridList", "list")
         .domain("www.vilanova.com.br")
         .path("/")
         .isHttpOnly(false)
         .isSecure(false)
         .build();
      this.cookiesWD.add(cookie);

      Cookie cookie2 = new Cookie.Builder("aceite_politicas_cookie", "2022-04-12%2011:05:47")
         .domain("www.vilanova.com.br")
         .path("/")
         .isHttpOnly(false)
         .isSecure(false)
         .build();

      this.cookiesWD.add(cookie);
      this.cookiesWD.add(cookie2);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.log("Página " + this.currentPage);

      this.pageSize = 24;
      String url = "https://www.vilanova.com.br/Busca/Resultado/?p="
         + this.currentPage
         + "&loja=&q="
         + this.keywordEncoded
         + "&ordenacao=6&limit=24&avancado=true";

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".card-product");

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }
         int alternativePosition = 1;

         for (Element product : products) {
            String internalPid = String.valueOf(CrawlerUtils.scrapIntegerFromHtmlAttr(product, null, "id", null));
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "p.product-name > a", "href");
            String name = CrawlerUtils.scrapStringSimpleInfo(product, ".product-name", false);


            Elements variations = product.select(".sku-variation-content .owl-item");

            if (!variations.isEmpty()) {
               for (Element variation : variations) {
                  String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(variation, ".picking", "data-sku-id");
                  Integer price = CrawlerUtils.scrapIntegerFromHtmlAttr(variation, ".picking", "data-preco-por", null);
                  String imageUrl = scrapImageUrl(variation);
                  String variationName = assembleName(name, variation);
                  boolean available = price != null;

                  RankingProduct productRanking = RankingProductBuilder.create()
                     .setUrl(productUrl)
                     .setInternalId(internalId)
                     .setInternalPid(internalPid)
                     .setImageUrl(imageUrl)
                     .setName(variationName)
                     .setPriceInCents(price)
                     .setAvailability(available)
                     .setPosition(alternativePosition)
                     .build();

                  saveDataProduct(productRanking);
               }
            }

            alternativePosition++;
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

   private String assembleName(String name, Element variation) {
      String variationName = CrawlerUtils.scrapStringSimpleInfo(variation, ".caixa-com", false);
      if (variationName != null && !variationName.isEmpty()) {
         name += " " + variationName;
      }
      return name.trim();
   }

   private String scrapImageUrl(Element variation) {
      String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(variation, ".picking", "data-foto");

      if (imageUrl != null && !imageUrl.isEmpty()) {
         imageUrl = imageUrl.replace("/200x200/", "/1000x1000/");
      }

      return imageUrl;
   }

   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".qtd-produtos", true, 0);
      this.log("Total da busca: " + this.totalProducts);
   }

   public String getCnpj() {
      return session.getOptions().optString("cnpj");
   }

   public String getPassword() {
      return session.getOptions().optString("password");
   }

   @Override
   protected Document fetchDocument(String url) {
      Document doc = new Document("");

      try {
         Logging.printLogDebug(logger, session, "Fetching page with webdriver...");

         webdriver = DynamicDataFetcher.fetchPageWebdriver(url, ProxyCollection.BUY_HAPROXY, session, this.cookiesWD, HOME_PAGE);
         doc = Jsoup.parse(webdriver.getCurrentPageSource());

         if (doc.select("body").isEmpty()) {
            webdriver = DynamicDataFetcher.fetchPageWebdriver(url, ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY, session, this.cookiesWD, HOME_PAGE);
            doc = Jsoup.parse(webdriver.getCurrentPageSource());
         }

         webdriver.waitLoad(30000);

         if (doc.selectFirst(".btn-politicas-cookies") != null) {
            Logging.printLogDebug(logger, session, "Achado botão de cookies");
            WebElement clickCookies = webdriver.driver.findElement(By.cssSelector("button.btn-politicas-cookies"));
            webdriver.clickOnElementViaJavascript(clickCookies);
            Logging.printLogDebug(logger, session, "Clicado no botão 1");

            if (doc.selectFirst("a.cc-ALLOW") != null) {
               waitForElement(webdriver.driver, "a.cc-ALLOW");
               WebElement allow = webdriver.driver.findElement(By.cssSelector("a.cc-btn.cc-ALLOW"));
               webdriver.clickOnElementViaJavascript(allow);
               Logging.printLogDebug(logger, session, "Clicado no botão 2");
            }
         } else {
            Logging.printLogDebug(logger, session, "Não foi possível achar o botão de cookies");
         }

         webdriver.waitLoad(15000);
         Logging.printLogDebug(logger, session, "Procurando botão de abrir login");

         WebElement openlogin = webdriver.driver.findElement(By.cssSelector("a.open-login"));
         webdriver.clickOnElementViaJavascript(openlogin);
         Logging.printLogDebug(logger, session, "Clicadno botão de abrir login");

         waitForElement(webdriver.driver, "#fazer-login");
         webdriver.findAndClick("#fazer-login", 2000);

         Logging.printLogDebug(logger, session, "Sending credentials...");

         webdriver.waitLoad(2000);
         waitForElement(webdriver.driver, "#usuarioCnpj");
         WebElement username = webdriver.driver.findElement(By.cssSelector("#usuarioCnpj"));
         username.sendKeys(getCnpj());

         webdriver.waitLoad(2000);
         waitForElement(webdriver.driver, "#usuarioSenha");
         WebElement pass = webdriver.driver.findElement(By.cssSelector("#usuarioSenha"));
         pass.sendKeys(getPassword());

         Logging.printLogDebug(logger, session, "awaiting login button");
         webdriver.waitLoad(5000);

         waitForElement(webdriver.driver, "#realizar-login");
         webdriver.findAndClick("#realizar-login", 15000);

         webdriver.waitForElement(".product-price", 15);
         Logging.printLogDebug(logger, session, "awaiting product page");

         boolean logged = false;
         while (!logged) {
            doc = Jsoup.parse(webdriver.getCurrentPageSource());
            JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script", "window.dataLayer = window.dataLayer || []; window.dataLayer.push(", ");", false, true);

            if (json.has("userId")) {
               logged = true;
            } else {
               webdriver.waitLoad(5000);
            }
         }

         return doc;

      } catch (Exception e) {
         Logging.printLogDebug(logger, session, CommonMethods.getStackTrace(e));
         Logging.printLogWarn(logger, "login não realizado");
      }

      return doc;
   }

   public static void waitForElement(WebDriver driver, String cssSelector) {
      WebDriverWait wait = new WebDriverWait(driver, 20);
      wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(cssSelector)));
   }
}
