package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
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
import models.RatingsReviews;
import models.pricing.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.*;

public class BrasilDrogarianisseiCrawler extends Crawler {
   private static final String HOME_PAGE = "https://www.farmaciasnissei.com.br/";

   public BrasilDrogarianisseiCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   protected Object fetch() {

      Logging.printLogDebug(logger, session, "Fetching page with webdriver...");
      ChromeOptions options = new ChromeOptions();
      options.addArguments("--window-size=1920,1080");
      options.addArguments("--headless");
      options.addArguments("--no-sandbox");
      options.addArguments("--disable-dev-shm-usage");

      Document doc = new Document("");
      int attempt = 0;
      boolean sucess = false;
      List<String> proxies = List.of(ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY, ProxyCollection.BUY_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY, ProxyCollection.BUY_HAPROXY);
      do {
         try {
            Logging.printLogDebug(logger, session, "Fetching page with webdriver...");

            webdriver = DynamicDataFetcher.fetchPageWebdriver(session.getOriginalURL(), proxies.get(attempt), session, this.cookiesWD, HOME_PAGE, options);
            webdriver.waitLoad(1000);

            doc = Jsoup.parse(webdriver.getCurrentPageSource());
            sucess = doc.selectFirst("div[data-produto_id]") != null;
            webdriver.terminate();
            attempt++;

         } catch (Exception e) {
            Logging.printLogDebug(logger, session, CommonMethods.getStackTrace(e));
            Logging.printLogWarn(logger, "Página não capturada");
         }

      } while (attempt < 3 && !sucess);

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

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "div[data-produto_id]", "data-produto_id");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".mt-3 h4", false);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".small a", true);
         String primaryImage = fixUrlImage(doc, internalId);
         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, ".dots-preview .swiper-slide img", Collections.singletonList("src"), "https", "www.farmaciasnissei.com.br", primaryImage);
         String description = CrawlerUtils.scrapElementsDescription(doc, List.of(".card #tabCollapse-descricao"));
         Offers offers = scrapOffers(doc);
         RatingsReviews ratingsReviews = getRatingsReviews(doc);
         List<String> eans = scrapEan(doc);

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setOffers(offers)
            .setEans(eans)
            .setRatingReviews(ratingsReviews)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private List<String> scrapEan(Document doc) {
      List<String> ean = new ArrayList<>();
      String productInfo = CrawlerUtils.scrapStringSimpleInfo(doc, "div .row div .mt-1", true);
      if (productInfo != null) {
         String[] split = productInfo.split("EAN:");
         if (split.length > 1) {
            ean.add(split[1].trim());
         }
      }

      return ean;
   }

   private boolean isProductPage(Document doc) {
      return !doc.select("div[data-produto_id]").isEmpty();
   }

   private String fixUrlImage(Document doc, String internalId) {
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".swiper-slide img", Collections.singletonList("src"), "https:", "www.farmaciasnissei.com.br");

      if (primaryImage.contains("caixa-nissei")) {
         return primaryImage.replace("caixa-nissei", internalId);

      }
      return primaryImage;
   }


   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = List.of(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName("Drogaria Nissei")
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;

   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".mt-md-2.mt-sm-2 > div > p", null, true, ',', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".mt-md-2.mt-sm-2 > div > span", null, true, ',', session);

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

      Installments installments = new Installments();
      if (installments.getInstallments().isEmpty()) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());
      }

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }


   /*
In this store, grades are given with a double value, e.g: 4.5 instead of 5 or 4.
Therefore, the crawler structure, by accepting only integer values, which is common on most sites, will not be captured the advanced rating.
 */
   private RatingsReviews getRatingsReviews(Document doc) {
      RatingsReviews ratingsReviews = new RatingsReviews();
      ratingsReviews.setDate(session.getDate());

      Integer reviews = CrawlerUtils.scrapIntegerFromHtml(doc, ".text-muted.font-xl", true, 0);
      ratingsReviews.setTotalWrittenReviews(reviews);
      ratingsReviews.setTotalRating(reviews);
      ratingsReviews.setAverageOverallRating(CrawlerUtils.scrapDoublePriceFromHtml(doc, ".avaliacao-produto .rating-produto", null, true, ',', session));

      return ratingsReviews;

   }


}
