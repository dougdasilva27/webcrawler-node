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
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.Pricing;
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
import java.util.List;

public class BrasilAmazonWDCrawler extends Crawler {


   protected BrasilAmazonWDCrawler(Session session) {
      super(session);
   }

   private final AmazonScraperUtils amazonScraperUtils = new AmazonScraperUtils(logger, session);

   Product product = null;
   private static final String HOST = "www.amazon.com.br";
   private static final String SELLER_NAME = "amazon.com.br";
   private static final String SELLER_NAME_2 = "amazon.com";
   private static final String SELLER_NAME_3 = "Amazon";

   private static final String IMAGES_HOST = "images-na.ssl-images-amazon.com";
   private static final String IMAGES_PROTOCOL = "https";


   protected Object fetch() {
      Document doc = null;
      Document docOffers;
      try {

         webdriver = DynamicDataFetcher.fetchPageWebdriver(session.getOriginalURL(), ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY, session);

         Logging.printLogInfo(logger, session, "awaiting product page load");

         webdriver.waitLoad(2000);

         webdriver.waitForElement(".a-icon.a-icon-arrow.a-icon-small.arrow-icon", 10000);

         doc = Jsoup.parse(webdriver.getCurrentPageSource());

         WebElement buyButtom = webdriver.driver.findElement(By.cssSelector(".a-icon.a-icon-arrow.a-icon-small.arrow-icon"));
         webdriver.clickOnElementViaJavascript(buyButtom);

         webdriver.waitForElement("#aod-offer-list", 10000);
         docOffers = Jsoup.parse(webdriver.getCurrentPageSource());

         product = extractProduct(doc, docOffers);

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


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      products.add(product);

      return products;
   }

   private Product extractProduct(Document doc, Document docOffers) throws MalformedProductException, OfferException, MalformedPricingException {
      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

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
         Offers offers = scrapOffers(doc, docOffers);

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
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return product;
   }


   private boolean isProductPage(Document doc) {
      return doc.select("#dp").first() != null;
   }


   public Offers scrapOffers(Document doc, Document offerPage) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      int pos = 1;

      Elements buyBox = doc.select(".a-box.mbc-offer-row.pa_mbc_on_amazon_offer");

      if (buyBox != null && !buyBox.isEmpty()) {
         for (Element oferta : buyBox) {

            String name = CrawlerUtils.scrapStringSimpleInfo(oferta, ".a-size-small.mbcMerchantName", true);

            Pricing pricing = amazonScraperUtils.scrapSellersPagePricingInBuyBox(oferta);
            String sellerUrl = CrawlerUtils.scrapUrl(oferta, ".a-size-small.a-link-normal:first-child", "href", "https", HOST);

            String sellerId = amazonScraperUtils.scrapSellerIdByUrl(sellerUrl);
            boolean isMainRetailer = name.equalsIgnoreCase(SELLER_NAME) || name.equalsIgnoreCase(SELLER_NAME_2) || name.equalsIgnoreCase(SELLER_NAME_3);

            if (sellerId == null) {
               sellerId = CommonMethods.toSlug(SELLER_NAME);
            }

            offers.add(Offer.OfferBuilder.create()
               .setInternalSellerId(sellerId)
               .setSellerFullName(name)
               .setSellersPagePosition(pos)
               .setIsBuybox(false)
               .setIsMainRetailer(isMainRetailer)
               .setPricing(pricing)
               .build());

            pos++;
         }
      }


      if (offerPage != null) {
         Elements ofertas = offerPage.select("#aod-offer");
         for (Element oferta : ofertas) {

            String name = amazonScraperUtils.scrapSellerName(oferta).trim();

            Pricing pricing = amazonScraperUtils.scrapSellersPagePricing(oferta);
            String sellerUrl = CrawlerUtils.scrapUrl(oferta, ".a-size-small.a-link-normal:first-child", "href", "https", HOST);

            String sellerId = amazonScraperUtils.scrapSellerIdByUrl(sellerUrl);

            boolean isMainRetailer = name.equalsIgnoreCase(SELLER_NAME) || name.equalsIgnoreCase(SELLER_NAME_2) || name.equalsIgnoreCase(SELLER_NAME_3);

            if (sellerId == null) {
               sellerId = CommonMethods.toSlug(SELLER_NAME);
            }

            if (!offers.contains(sellerId)) {

               offers.add(Offer.OfferBuilder.create()
                  .setInternalSellerId(sellerId)
                  .setSellerFullName(name)
                  .setSellersPagePosition(pos)
                  .setIsBuybox(false)
                  .setIsMainRetailer(isMainRetailer)
                  .setPricing(pricing)
                  .build());

               pos++;
            }
         }

      }
      return offers;
   }


}
