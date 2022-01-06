package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.AmazonScraperUtils;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import org.json.JSONArray;
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
import java.util.Random;

public class BrasilAmazonWDCrawler extends Crawler {

   public BrasilAmazonWDCrawler(Session session) {
      super(session);
   }

   private final AmazonScraperUtils amazonScraperUtils = new AmazonScraperUtils(logger, session);

   Product product = null;

   private static final String IMAGES_HOST = "images-na.ssl-images-amazon.com";
   private static final String IMAGES_PROTOCOL = "https";

   private static final List<String> ProxyList = Arrays.asList(
      ProxyCollection.BUY_HAPROXY,
      ProxyCollection.LUMINATI_SERVER_BR_HAPROXY,
      ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
      ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
      ProxyCollection.NETNUT_RESIDENTIAL_DE_HAPROXY,
      ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY,
      ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY
   );

   protected Object fetch() {
      Random random = new Random();
      Document doc = null;
      Document docOffers;

      try {

         webdriver = DynamicDataFetcher.fetchPageWebdriver(session.getOriginalURL(), ProxyList.get(random.nextInt(ProxyList.size())), session);

         Logging.printLogInfo(logger, session, "awaiting product page load");


         webdriver.waitForElement("#dp", 5000);


         doc = Jsoup.parse(webdriver.getCurrentPageSource());

         String clickOffers = null;

         if (!doc.select(AmazonScraperUtils.listSelectors.get("iconArrowOffer")).isEmpty()) {
            clickOffers = AmazonScraperUtils.listSelectors.get("iconArrowOffer");
         } else if (!doc.select(AmazonScraperUtils.listSelectors.get("linkOffer")).isEmpty()) {
            clickOffers = AmazonScraperUtils.listSelectors.get("linkOffer");
         }

         if (clickOffers != null) {
            WebElement buyButtom = webdriver.driver.findElement(By.cssSelector(clickOffers));
            webdriver.waitForElement(clickOffers, 1000);
            webdriver.clickOnElementViaJavascript(buyButtom);

            webdriver.waitForElement("#aod-offer-list", 5000);
            loadAllOffers();

            docOffers = Jsoup.parse(webdriver.getCurrentPageSource());

            product = extractProduct(doc, docOffers);

         }


      } catch (Exception e) {
         Logging.printLogInfo(logger, session, CommonMethods.getStackTrace(e));
         webdriver.terminate();
      }

      return doc;
   }

   public static void waitForElement(WebDriver driver, String cssSelector) {
      WebDriverWait wait = new WebDriverWait(driver, 900);
      wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(cssSelector)));
   }

   private void loadAllOffers() {
     List<WebElement> webElementList = webdriver.findElementsByCssSelector("#aod-offer");
     int listSize = webElementList.size();

     boolean finish = false;
     WebElement container = webdriver.findElementByCssSelector("#all-offers-display-scroller");


     do {
        WebElement webElement = webElementList.get(listSize - 1);
        webdriver.scrollToElement(webElement);
        webdriver.waitLoad(5000);
        webElementList = webdriver.findElementsByCssSelector("#aod-offer");
        int currentListSize = webElementList.size();
        if (currentListSize != listSize){
           listSize = currentListSize;
        } else {
           finish = true;
        }
     } while (!finish);

   }



   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();
      if (product != null) {
         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not have page offers " + this.session.getOriginalURL());
      }

      return products;
   }

   private Product extractProduct(Document doc, Document docOffers) throws MalformedProductException, OfferException, MalformedPricingException {

      String internalId = amazonScraperUtils.crawlInternalId(doc);
      String internalPid = internalId;
      String name = amazonScraperUtils.crawlName(doc);
      CategoryCollection categories = amazonScraperUtils.crawlCategories(doc);

      JSONArray images = this.amazonScraperUtils.scrapImagesJSONArray(doc);
      String primaryImage = this.amazonScraperUtils.scrapPrimaryImage(images, doc, IMAGES_PROTOCOL, IMAGES_HOST);
      List<String> secondaryImages = this.amazonScraperUtils.scrapSecondaryImages(images, IMAGES_PROTOCOL, IMAGES_HOST);

      String description = amazonScraperUtils.crawlDescription(doc);
      Integer stock = null;
      List<String> eans = amazonScraperUtils.crawlEan(doc);
      Offer mainPageOffer = amazonScraperUtils.scrapMainPageOffer(doc);
      Offers offers = scrapOffers(doc, docOffers, mainPageOffer);

      RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();
      ratingReviewsCollection.addRatingReviews(amazonScraperUtils.crawlRating(doc, internalId));
      RatingsReviews ratingReviews = ratingReviewsCollection.getRatingReviews(internalId);

      product = ProductBuilder.create()
         .setUrl(session.getOriginalURL())
         .setInternalId(internalId)
         .setInternalPid(internalPid)
         .setName(name)
         .setCategory1(categories.getCategory(0))
         .setCategory2(categories.getCategory(1))
         .setCategory3(categories.getCategory(2))
         .setPrimaryImage(primaryImage)
         .setSecondaryImages(secondaryImages)
         .setDescription(description)
         .setStock(stock)
         .setEans(eans)
         .setRatingReviews(ratingReviews)
         .setOffers(offers)
         .build();

      return product;
   }

   public Offers scrapOffers(Document doc, Document offerPage, Offer mainPageOffer) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      int pos = 1;

      if (mainPageOffer != null) {
         mainPageOffer.setSellersPagePosition(pos);
         offers.add(mainPageOffer);
         pos = 2;
      }

      Elements buyBox = doc.select(".a-box.mbc-offer-row.pa_mbc_on_amazon_offer");

      if (buyBox != null && !buyBox.isEmpty()) {
         for (Element oferta : buyBox) {
            amazonScraperUtils.getOffersFromBuyBox(oferta, pos, offers);
            pos++;

         }
      }

      if (offerPage != null) {
         Elements ofertas = offerPage.select("#aod-offer");
         for (Element oferta : ofertas) {

            amazonScraperUtils.getOffersFromOfferPage(oferta, pos, offers);

            pos++;
         }
      }

      return offers;
   }


}
