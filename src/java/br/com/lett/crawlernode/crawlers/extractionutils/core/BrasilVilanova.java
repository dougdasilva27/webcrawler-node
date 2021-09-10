package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static java.util.Collections.singletonList;

public class BrasilVilanova extends Crawler {

   public static final String HOME_PAGE = "https://www.vilanova.com.br/";
   private static final String IMAGES_HOST = "i2-vilanova.a8e.net.br";

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public BrasilVilanova(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.WEBDRIVER);
   }

   public String getCnpj() {
      return session.getOptions().optString("cnpj");
   }

   public String getPassword() {
      return session.getOptions().optString("password");

   }

   public String getSellerFullname() {
      return session.getOptions().optString("seller");

   }

   @Override
   protected Object fetch() {

      Document doc = new Document("");

      try {
         Logging.printLogDebug(logger, session, "Fetching page with webdriver...");

         webdriver = DynamicDataFetcher.fetchPageWebdriver(session.getOriginalURL(), ProxyCollection.LUMINATI_SERVER_BR_HAPROXY, session);
         doc = Jsoup.parse(webdriver.getCurrentPageSource());
         webdriver.waitLoad(10000);

         if (doc.selectFirst("button.btn-politicas-cookies") != null) {
            waitForElement(webdriver.driver, "button.btn-politicas-cookies");
            WebElement clickCookies = webdriver.driver.findElement(By.cssSelector("button.btn-politicas-cookies"));
            webdriver.clickOnElementViaJavascript(clickCookies);
            waitForElement(webdriver.driver, "a.cc-ALLOW");
            WebElement allow = webdriver.driver.findElement(By.cssSelector("a.cc-btn.cc-ALLOW"));
            webdriver.clickOnElementViaJavascript(allow);
         }

         webdriver.waitLoad(10000);
         waitForElement(webdriver.driver, "button.open-login");
         WebElement openlogin = webdriver.driver.findElement(By.cssSelector("button.open-login"));
         webdriver.clickOnElementViaJavascript(openlogin);
         waitForElement(webdriver.driver, "button[id=fazer-login]");
         webdriver.findAndClick("button[id=fazer-login]", 2000);


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
         webdriver.waitLoad(4000);

         waitForElement(webdriver.driver, "#realizar-login");
         webdriver.findAndClick("#realizar-login", 15000);

         webdriver.waitForElement(".product-details-body", 10000);
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

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script", "window.dataLayer = window.dataLayer || []; window.dataLayer.push(", ");", false, true);

         if (json != null && !json.isEmpty()) {
            JSONObject jsonProduct = json.optJSONObject("productData");

            String internalPid = jsonProduct.optString("productID");
            List<String> eans = singletonList(CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".variacao-container", "data-produtoean"));
            CategoryCollection categories = scrapCategories(jsonProduct);
            String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".tab-content"));
            //cannot find any secondary image
            Elements variations = doc.select(".product-details-body  .owl-item.active");

            for (Element variation : variations) {
               String name = getName(doc, variation);
               String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(variation, ".item.picking", "data-sku-id");
               String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(variation, ".item", Arrays.asList("data-foto"), "https", "www.vilanova.com.br");

               boolean isAvailable = doc.selectFirst(".product-details-footer  .btn.btn-primary.btn-comprar-produto") != null;
               Offers offers = isAvailable ? scrapOffers(variation) : new Offers();

               Product product = ProductBuilder.create()
                  .setUrl(session.getOriginalURL())
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setOffers(offers)
                  .setCategories(categories)
                  .setPrimaryImage(primaryImage)
                  .setDescription(description)
                  .setEans(eans)
                  .build();

               products.add(product);

            }
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return !doc.select("div.product-row").isEmpty();
   }

   private String getName(Document document, Element variation) {
      StringBuilder nameBuilder = new StringBuilder();
      String name = CrawlerUtils.scrapStringSimpleInfo(document, ".product-title", true);
      String quantity = CrawlerUtils.scrapStringSimpleInfo(variation, ".picking-quantity  span", true);

      if (name != null) {
         nameBuilder.append(name);

         if (quantity != null) {
            nameBuilder.append(" - ").append(quantity);
         }
      }

      return nameBuilder.toString();

   }

   private CategoryCollection scrapCategories(JSONObject json) {
      CategoryCollection categories = new CategoryCollection();
      String categoriesStr = json.optString("categoryTree");

      if (categoriesStr != null) {
         String[] categorieList = categoriesStr.split("/");
         if (categorieList.length != 0) {
            categories.addAll(Arrays.asList(categorieList));
         }
      }

      return categories;
   }

   private Offers scrapOffers(Element product) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(product);
      List<String> sales = new ArrayList<>(); // When this new offer model was implemented, no sales was found

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(getSellerFullname())
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());


      return offers;
   }

   private Pricing scrapPricing(Element product) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(product, ".item", "data-preco-de", true, ',', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(product, ".item", "data-preco-por", true, ',', session);

      if (priceFrom != null && priceFrom.equals(spotlightPrice)) {
         priceFrom = null;
      }

      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = scrapCreditcards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditcards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());


      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }
}
