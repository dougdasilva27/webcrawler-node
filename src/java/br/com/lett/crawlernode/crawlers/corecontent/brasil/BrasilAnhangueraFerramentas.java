package br.com.lett.crawlernode.crawlers.corecontent.brasil;

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
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class BrasilAnhangueraFerramentas extends Crawler {
   protected BrasilAnhangueraFerramentas(Session session) {
      super(session);
   }

   private static final String SELLER_FULL_NAME = "Anhanguera Ferramentas (Brasil)";
   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString());

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      String homePage = "https://www.anhangueraferramentas.com.br";
      return !FILTERS.matcher(href).matches() && (href.startsWith(homePage));
   }

   @Override
   protected Object fetch() {
      Document doc = null;
      try {
         webdriver = DynamicDataFetcher.fetchPageWebdriver(session.getOriginalURL(), ProxyCollection.BUY_HAPROXY, session);
         doc = Jsoup.parse(webdriver.getCurrentPageSource());

      } catch (Exception e) {
         Logging.printLogInfo(logger, session, CommonMethods.getStackTrace(e));
      }
      return doc;
   }

   public static void waitForElement(WebDriver driver, String cssSelector) {
      WebDriverWait wait = new WebDriverWait(driver, 90);
      wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(cssSelector)));
   }


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogInfo(
            logger, session, "Product page identified: " + session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, ".fbits-sku", true);
         String internalPid =  CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#hdnProdutoId", "value");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".produto-detalhe-content h1", true);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".order-last .fbits-breadcrumb li", true);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc,".main-img .slick-active .zoomImg", Collections.singletonList("src"), "https", "anhangueraferramentas.fbitsstatic.net");
         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, ".main-img .slick-slide .zoomImg", Collections.singletonList("src"), "https", "imgprd.martins.com.br", primaryImage);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".infoProd"));

         RatingsReviews ratingsReviews = scrapRating(doc, internalId);
         boolean available = !doc.select(".js-add-to-cart").isEmpty();
         Offers offers = available ? scrapOffers(doc) : new Offers();


         // Creating the product
         Product product =
            ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setName(name)
               .setCategory1(categories.getCategory(0))
               .setCategory2(categories.getCategory(1))
               .setCategory3(categories.getCategory(2))
               .setPrimaryImage(primaryImage)
               .setRatingReviews(ratingsReviews)
               .setSecondaryImages(secondaryImages)
               .setOffers(offers)
               .setDescription(description)
               .setEans(eans)
               .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + session.getOriginalURL());
      }

      return products;
   }

   private RatingsReviews scrapRating(Document doc, String internalId) {
      RatingsReviews ratingsReviews = new RatingsReviews();
      String ratingString =
         CrawlerUtils.scrapStringSimpleInfoByAttribute(
            doc, ".hidden-sm .rating .rating-stars", "data-rating");
      JSONObject jsonRating = JSONUtils.stringToJson(ratingString);

      double avgRating = jsonRating.opt("rating") != null ? jsonRating.optInt("rating") : 0;
      int totalReviews = CrawlerUtils.scrapIntegerFromHtml(doc, ".box-review > div:nth-child(3) u", true, 0);

      ratingsReviews.setTotalRating(totalReviews);
      ratingsReviews.setTotalWrittenReviews(totalReviews);
      ratingsReviews.setAverageOverallRating(avgRating);
      return ratingsReviews;
   }

   private boolean isProductPage(Document doc) {
      return !doc.select(".page-produto").isEmpty();
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      //Site hasn't any sale

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .build());

      return offers;

   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".qdValue .value", null, true, ',', session);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      //Site hasn't any product with old price

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
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
