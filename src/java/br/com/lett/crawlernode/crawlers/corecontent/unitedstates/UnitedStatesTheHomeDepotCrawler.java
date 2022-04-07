package br.com.lett.crawlernode.crawlers.corecontent.unitedstates;

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
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebElement;

import java.util.*;

public class UnitedStatesTheHomeDepotCrawler extends Crawler {
   private static final List<String> cards = Arrays.asList(Card.MASTERCARD.toString());
   private static final String SELLER_NAME = "The Home Depot";
   private String CURRENT_URL = null;
   private Integer VARIATION = 1;
   private static final String HOME_PAGE = "https://www.homedepot.com/";
   private Document currentDoc;

   public UnitedStatesTheHomeDepotCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.APACHE);   }

   @Override
   public void handleCookiesBeforeFetch() {
      String localizer = session.getOptions().optString("localizer");

      Cookie cookie = new Cookie.Builder("THD_LOCALIZER", localizer)
         .domain(".homedepot.com")
         .path("/")
         .isHttpOnly(false)
         .isSecure(false)
         .build();
      this.cookiesWD.add(cookie);
   }

   @Override
   protected Document fetch() {
      Document doc = null;

      try {
         if (this.VARIATION == 1) {
            webdriver = DynamicDataFetcher.fetchPageWebdriver(session.getOriginalURL(), ProxyCollection.NETNUT_RESIDENTIAL_US_HAPROXY, session, this.cookiesWD, this.HOME_PAGE);
            webdriver.waitForElement("h1.product-details__title", 120);

            webdriver.waitLoad(70000);
            this.CURRENT_URL = this.session.getOriginalURL();
         } else {
            webdriver.waitForElement(".super-sku__inline-attribute-wrapper", 120);

            String variationButtonString = CrawlerUtils.scrapStringSimpleInfo(this.currentDoc, ".super-sku__inline-attribute-wrapper div:nth-child(" + this.VARIATION + ") button", false);
            WebElement variationButton;

            if (variationButtonString != null) {
               variationButton = webdriver.driver.findElement(
                  By.cssSelector(".super-sku__inline-attribute-wrapper div:nth-child(" + this.VARIATION + ") button"));
            } else {
               variationButton = webdriver.driver.findElement(
                  By.cssSelector(".super-sku__inline-attribute-wrapper button:nth-child(" + this.VARIATION + ")"));
            }

            webdriver.clickOnElementViaJavascript(variationButton);
            webdriver.waitLoad(70000);

            this.CURRENT_URL = webdriver.getCurURL();
         }
         doc = Jsoup.parse(webdriver.getCurrentPageSource());

      } catch (Exception e) {
         Logging.printLogInfo(logger, session, CommonMethods.getStackTrace(e));
         webdriver.terminate();
      }
      return doc;
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (doc != null) {
         Elements variations = scrapVariation(doc);

         for (int i = 0; i < variations.size(); i++) {
            this.currentDoc = fetchVariation(doc);
            JSONObject productJson = scrapJsonFromHtml(this.currentDoc);

            if (productJson != null) {
               Logging.printLogDebug(logger, session, "Product page identified: " + this.CURRENT_URL);

               String internalId = productJson.optString("productID");
               String name = productJson.optString("name");
               String description = productJson.optString("description");
               JSONArray images = productJson.optJSONArray("image");
               String primaryImage = scrapFirstImage(images);
               List<String> secondaryImages = scrapSecondaryImages(images);
               CategoryCollection categories = scrapCategories(this.currentDoc);
               boolean available = scrapAvailability(this.currentDoc);
               RatingsReviews ratingsReviews = scrapRatingReviews(productJson);

               Offers offers = available ? scrapOffers(productJson) : new Offers();

               Product product = ProductBuilder.create()
                  .setUrl(this.CURRENT_URL)
                  .setInternalId(internalId)
                  .setInternalPid(internalId)
                  .setName(name)
                  .setOffers(offers)
                  .setCategories(categories)
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(secondaryImages)
                  .setDescription(description)
                  .setRatingReviews(ratingsReviews)
                  .build();

               products.add(product);
            }
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page:   " + this.session.getOriginalURL());
      }

      return products;
   }

   private String scrapFirstImage(JSONArray imagesArray) {
      if (imagesArray != null && imagesArray.length() > 1) {
         Object image = imagesArray.get(0);
         return image.toString();
      }
      return null;
   }

   private List<String> scrapSecondaryImages(JSONArray imagesArray) {
      List<String> secondaryImages = new ArrayList<>();

      if (imagesArray != null && imagesArray.length() > 2) {
         for (int i = 1; i < imagesArray.length(); i++) {
            secondaryImages.add(imagesArray.get(i).toString());
         }
      }

      return secondaryImages;
   }

   private Boolean scrapAvailability(Document doc) {
      Element availability = doc.selectFirst(".buybox__actions");

      if (availability != null) {
         return true;
      }

      return false;
   }

   private Offers scrapOffers(JSONObject productJson) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(productJson);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));

      offers.add(new Offer.OfferBuilder()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(this.SELLER_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Pricing scrapPricing(JSONObject productJson) throws MalformedPricingException {
      Integer priceInt = JSONUtils.getValueRecursive(productJson, "offers.price", Integer.class);
      Double spotlightPrice = null;
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(this.currentDoc, ".price-detailed__was-price span.u__strike span", null, false, '.', session);

      if (priceInt != null) {
         spotlightPrice = Double.valueOf(priceInt);
      } else {
         spotlightPrice = JSONUtils.getValueRecursive(productJson, "offers.price", Double.class);
      }

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .build();
   }


   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      Integer installmentNumber = 1;
      Double finalPrice = spotlightPrice;
      Double installmentPrice = spotlightPrice;

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(installmentNumber)
         .setInstallmentPrice(installmentPrice)
         .setFinalPrice(finalPrice)
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

   private Document fetchVariation(Document doc) {
      if (this.VARIATION != 1) {
            doc = fetch();
      }
      this.VARIATION++;
      return doc;
   }

   private Elements scrapVariation(Document doc) {
      Element variationsDiv = doc.selectFirst(".super-sku__inline-attribute-wrapper--tile");
      Elements variations = new Elements();

      if (variationsDiv != null) {
         variations = variationsDiv.select("div.super-sku__inline-tile--space button");
      } else {
         variations = doc.select(".super-sku__inline-attribute .super-sku__inline-attribute-wrapper button");
      }

      if (variations.size() == 0) {
         variations = doc.select(".product-details__title");
      }

      return variations;
   }

   JSONObject scrapJsonFromHtml (Document doc) {
      JSONObject productJson = CrawlerUtils.selectJsonFromHtml(doc, "script[data-th=\"server\"]", null, null, false, false);

      if (productJson == null) {
         return null;
      }

      if (productJson.isEmpty()) {
         productJson = CrawlerUtils.selectJsonFromHtml(doc, "script[data-th=\"client\"]", null, null, false, false);
      }

      return productJson;
   }

   private RatingsReviews scrapRatingReviews(JSONObject json) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      JSONArray reviews = json.optJSONArray("review");

      Integer totalNumOfEvaluations = reviews.length();
      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(reviews);
      Double avgRating = CrawlerUtils.extractRatingAverageFromAdvancedRatingReview(advancedRatingReview);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);

      return ratingReviews;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(JSONArray reviews) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;


      if (reviews != null) {

         for (Object rating : reviews) {
            Integer star = JSONUtils.getValueRecursive(rating, "reviewRating.ratingValue", Integer.class);

            switch (star) {
               case 1:
                  star1++;
                  break;
               case 2:
                  star2++;
                  break;
               case 3:
                  star3++;
                  break;
               case 4:
                  star4++;
                  break;
               case 5:
                  star5++;
                  break;
               default:
                  break;
            }
         }
      }

      return new AdvancedRatingReview.Builder()
         .totalStar1(star1)
         .totalStar2(star2)
         .totalStar3(star3)
         .totalStar4(star4)
         .totalStar5(star5)
         .build();
   }

   protected CategoryCollection scrapCategories(Document doc) {
      CategoryCollection categories = new CategoryCollection();
      Elements breadcrumbs = doc.select("[name=\"breadcrumbs\"] a");

      for (Element category : breadcrumbs) {
         String categoryName = CrawlerUtils.scrapStringSimpleInfo(category,"a", false);
         categories.add(categoryName);
      }

      return categories;
   }
}
