package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.*;

public class BrasilPeixotoCrawler extends Crawler {

   public BrasilPeixotoCrawler(Session session) {
      super(session);
      config.setFetcher(FetchMode.JSOUP);
      config.setParser(Parser.HTML);
   }

   public void getCookiesFromWD(String proxy) {
      try {
         Logging.printLogDebug(logger, session, "Fetching page with webdriver...");

         webdriver = DynamicDataFetcher.fetchPageWebdriver("https://www.peixoto.com.br/customer/account/login/", proxy, session);

         webdriver.waitLoad(1000);

         waitForElement(webdriver.driver, ".page-main #email");
         WebElement username = webdriver.driver.findElement(By.cssSelector(".page-main #email"));
         username.sendKeys(session.getOptions().optString("user"));

         webdriver.waitLoad(2000);
         waitForElement(webdriver.driver, ".page-main #pass");
         WebElement pass = webdriver.driver.findElement(By.cssSelector(".page-main #pass"));
         pass.sendKeys(session.getOptions().optString("pass"));

         waitForElement(webdriver.driver, ".page-main button.login");
         webdriver.findAndClick(".page-main button.login", 4000);

         //chose catalão - GO
         waitForElement(webdriver.driver, "#branch-select option[value='5']");
         webdriver.findAndClick("#branch-select option[value='5']", 4000);

         waitForElement(webdriver.driver, "option[value=\"pagamento_antecipado\"]");
         webdriver.findAndClick("option[value=\"pagamento_antecipado\"]", 2000);

         waitForElement(webdriver.driver, "button.b2b-choices");
         webdriver.findAndClick("button.b2b-choices", 4000);

         Set<Cookie> cookiesResponse = webdriver.driver.manage().getCookies();

         for (Cookie cookie : cookiesResponse) {
            BasicClientCookie basicClientCookie = new BasicClientCookie(cookie.getName(), cookie.getValue());
            basicClientCookie.setDomain(cookie.getDomain());
            basicClientCookie.setPath(cookie.getPath());
            basicClientCookie.setExpiryDate(cookie.getExpiry());
            this.cookies.add(basicClientCookie);
         }

      } catch (Exception e) {
         Logging.printLogDebug(logger, session, CommonMethods.getStackTrace(e));
         Logging.printLogWarn(logger, "login não realizado");
      } finally {
         if (webdriver != null) {
            webdriver.terminate();
         }
      }
   }

   @Override
   protected Response fetchResponse() {

      int attemp = 0;

      List<String> proxies = Arrays.asList(
         ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
         ProxyCollection.BUY_HAPROXY,
         ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
         ProxyCollection.SMART_PROXY_BR_HAPROXY,
         ProxyCollection.LUMINATI_SERVER_BR_HAPROXY
      );

      do {
         getCookiesFromWD(proxies.get(attemp));
      } while (attemp++ < (proxies.size() -1));

      Map<String, String> headers = new HashMap<>();
      headers.put("authority", "www.peixoto.com.br");
      headers.put("accept-language", "en-US,en;q=0.9,pt;q=0.8,pt-PT;q=0.7");
      headers.put("referer", "https://www.peixoto.com.br/cms/index/index");
      headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");

      Request request = Request.RequestBuilder.create()
         .setUrl(session.getOriginalURL())
         .setHeaders(headers)
         .setCookies(cookies)
         .setProxyservice(proxies)
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher(), new FetcherDataFetcher()), session);

      return response;

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

         String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, "div.value", true);
         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "div.price-final_price", "data-product-id");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "span[itemprop=name]", true);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("div.product.pricing"));
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "ul.items a", false);
         List<String> imagesList = getImageListFromScript(doc);
         String primaryImage = imagesList != null ? imagesList.remove(0) : null;

         boolean availableToBuy = doc.select("button[id=button-out]").isEmpty();
         Offers offers = availableToBuy ? scrapOffers(doc) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(imagesList)
            .setOffers(offers)
            .setDescription(description)

            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private List<String> getImageListFromScript(Document doc) {
      Element imageScript = doc.selectFirst("script:containsData(mage/gallery/gallery)");
      if (imageScript != null) {
         JSONObject imageToJson = CrawlerUtils.stringToJson(imageScript.html());
         JSONArray imageArray = JSONUtils.getValueRecursive(imageToJson, "[data-gallery-role=gallery-placeholder].mage/gallery/gallery.data", JSONArray.class);
         List<String> imagesList = new ArrayList<>();
         for (int i = 0; i < imageArray.length(); i++) {
            String imageList = JSONUtils.getValueRecursive(imageArray, i + ".img", String.class);
            imagesList.add(imageList);
         }
         return imagesList;
      }
      return null;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("div.product-main-content") != null;
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      List<String> sales = new ArrayList<>();
      Pricing pricing = scrapPricing(doc);

      offers.add(Offer.OfferBuilder.create()
         .setSellerFullName("peixoto")
         .setMainPagePosition(1)
         .setPricing(pricing)
         .setSales(sales)
         .setUseSlugNameAsInternalSellerId(true)
         .setIsMainRetailer(true)
         .setIsBuybox(false)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "span.price-wrapper  .price", null, true, ',', session);
      Double priceFrom = spotlightPrice;

      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString());

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
