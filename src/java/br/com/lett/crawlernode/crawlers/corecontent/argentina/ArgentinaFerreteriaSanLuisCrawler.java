package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Parser;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.net.HttpHeaders;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.Pricing;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArgentinaFerreteriaSanLuisCrawler extends Crawler {
   private static String SELLER_NAME = "Ferreteria San Luis";
   private static String HOST = "https://www.ferreterasanluis.com/";


   public ArgentinaFerreteriaSanLuisCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.HTML);
   }


   private Boolean verifyAvailable(String internalId) {
      HashMap<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.ORIGIN, "https://www.ferreterasanluis.com/");
      headers.put(HttpHeaders.REFERER, session.getOriginalURL());
      headers.put(HttpHeaders.ACCEPT, "*/*");
      Request request = Request.RequestBuilder.create()
         .setUrl("https://www.ferreterasanluis.com/cart/item-stock/" + internalId)
         .setHeaders(headers)
         .setProxyservice(List.of(
            ProxyCollection.BUY,
            ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
            ProxyCollection.LUMINATI_SERVER_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_DE_HAPROXY
         ))
         .build();
      List<DataFetcher> fetchers = List.of(new JsoupDataFetcher(), this.dataFetcher, new ApacheDataFetcher(), new FetcherDataFetcher());
      int attempt = 0;
      do {
         Response response = CrawlerUtils.retryRequest(request, session, fetchers.get(attempt), false);
         String stock = response.getBody();
         if (response.isSuccess() && !stock.isEmpty()) {
            return !stock.equals("0");
         }
         attempt++;
      } while (attempt < fetchers.size());
      Logging.printLogDebug(logger, session, "Request for availability failed" + this.session.getOriginalURL());
      return true;
   }
   public static void waitForElement(WebDriver driver, String cssSelector) {
      WebDriverWait wait = new WebDriverWait(driver, 10);
      wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(cssSelector)));
   }

   private Document webDriverLogin(){
      Document document = null;
      try {
         webdriver = DynamicDataFetcher.fetchPageWebdriver("https://www.ferreterasanluis.com/login", ProxyCollection.BUY_HAPROXY, session, this.cookiesWD, "https://www.ferreterasanluis.com");
         webdriver.waitForElement(".uk-container.tm-login", 30);

         webdriver.waitForElement( "#loginForm input[name=email]", 10);
         webdriver.sendToInput("#loginForm input[name=email]", session.getOptions().optString("email"), 10);

         webdriver.waitForElement( "#loginForm input[name=password]", 10);
         webdriver.sendToInput("#loginForm input[name=password]", session.getOptions().optString("password"), 10);

         webdriver.waitForElement( "#loginForm button", 10);
         webdriver.findAndClick("#loginForm button", 30000);

         webdriver.waitLoad(30000);
         webdriver.loadUrl(session.getOriginalURL());

         document = Jsoup.parse(webdriver.getCurrentPageSource());
      } catch (Exception e) {
         Logging.printLogInfo(logger, session, CommonMethods.getStackTrace(e));

      } finally {
         if (webdriver != null) {
            webdriver.terminate();
         }
      }
      return document;
   }
   @Override
   public List<Product> extractInformation(Document document) throws Exception {
      List<Product> products = new ArrayList<>();

      if (isProductPage(document)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         Boolean loginRequired = CrawlerUtils.scrapDoublePriceFromHtml(document, ".tm-final-price .tm-num", null, false, ',', session) == null;
         if (loginRequired){
            Document wdDocument = webDriverLogin();
            document = wdDocument != null ? wdDocument : document;
         }
         String productName = CrawlerUtils.scrapStringSimpleInfo(document, ".tm-container-section h1", false);
         String internalId = getInternalId(document);
         List<String> images = getImages(document);
         String primaryImage = images.size() > 0 ? images.remove(0) : null;
         String internalPid = getInternalPid(primaryImage);
         CategoryCollection categories = getCategories(document);
         String description = CrawlerUtils.scrapSimpleDescription(document, Arrays.asList(".tm-description"));
         boolean available = verifyAvailable(internalId);
         Offers offers = available ? scrapOffers(document) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid != null ? internalPid : internalId)
            .setName(productName)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
            .setDescription(description)
            .setCategories(categories)
            .setOffers(offers)
            .build();
         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }
      return products;
   }

   private String getInternalPid(String image_path) {
      if (image_path != null && !image_path.isEmpty()) {
         String id_str = CommonMethods.getLast(List.of(image_path.split("/")));
         if (id_str != null) {
            return CommonMethods.substring(id_str, "", ".", false);
         }
      }
      return null;
   }

   private CategoryCollection getCategories(Document doc) {
      String firstItem = CrawlerUtils.scrapStringSimpleInfo(doc, ".uk-breadcrumb .uk-visible-large span span", true);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".uk-breadcrumb a");
      categories.set(0, String.valueOf(firstItem));

      return categories;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("div.tm-page.tm-producto-ampliado > div.uk-container.uk-container-center.tm-container-section") != null;
   }

   private String getInternalId(Document doc) {
      String regex = "\\| (.*)";
      String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, ".tm-shortDesc p", true);

      Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      if (internalId != null && !internalId.isEmpty()) {
         Matcher matcher = pattern.matcher(internalId);
         if (matcher.find()) {
            return matcher.group(1);
         }
      }
      return null;
   }

   private List<String> getImages(Document doc) {
      List<String> images = new ArrayList<>();

      Elements imagesDivs = doc.select("#js-gallery-product li img");
      for (Element imageLi : imagesDivs) {
         images.add(HOST + imageLi.attr("src"));
      }
      return images;
   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));

      offers.add(new Offer.OfferBuilder()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Double getSpotlightPrice(Document doc) {
      Double price = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".tm-final-price .tm-num", null, false, ',', session);
      if (price != null) {
         return price / 100;
      }
      return null;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = getSpotlightPrice(doc);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".uk-clearfix .tm-regular-price span", null, false, '.', session);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .build();
   }
}

