package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.jsoup.nodes.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MexicoSingerCrawler extends Crawler {

   Double globalPrice = null;
   private static final Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.DINERS.toString(), Card.AMEX.toString(), Card.ELO.toString());

   public MexicoSingerCrawler(Session session) {
      super(session);
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

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#vue-pdp-page > div > div > div", "data-sku");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".col-xs-12.col-sm-12.col-md-6.pdp-content-wrapper.no-padding-left.no-padding-right > div > h1", false);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#collapseOne > div > p:first-child", "#collapseOne > div > p:last-child"));
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".simpleLens-container > div > a > img", Arrays.asList("src"), "http://", "www.singer.com/mx");
         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".simpleLens-thumbnails-container > div > a", Arrays.asList("data-big-image"), "http://", "www.singer.com/mx", primaryImage);
         String availableHolder = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "[property='product:price:amount']", "content");
         boolean available = availableHolder != null;
         Offers offers = available ? scrapOffers(doc) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);


      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".col-xs-12.col-sm-12.col-md-6.pdp-content-wrapper.no-padding-left.no-padding-right > div > h1") != null;
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(pricing);

      if (pricing != null) {
         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName("Singer")
            .setSellersPagePosition(1)
            .setSales(sales)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .build());
      }

      return offers;
   }

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();
      String sale = CrawlerUtils.calculateSales(pricing);
      if (sale != null) {
         sales.add(sale);
      }
      return sales;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {

      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "[property='product:price:amount']", "content", false, '.', session);
      Double priceFrom = scrapPriceFrom(doc, spotlightPrice);

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .build();
   }

   private Double scrapPriceFrom(Document doc, Double spot) {
      Double priceFrom = null;
      String holderPrice;
      Double price = null;
      String regex = "\\$(.*)en";
      String holder = CrawlerUtils.scrapStringSimpleInfo(doc, ".simpleLens-container > span > span:nth-child(even)", false);
      if (holder != null) {
         final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
         final Matcher matcher = pattern.matcher(holder);
         if (holder != null) {
            if (matcher.find()) {
               holderPrice = ((matcher.group(1)));
               price = Double.valueOf(holderPrice);
            }
            priceFrom = spot + price;
         }
      } else {
         priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "[property='product:price:amount']", "content", false, '.', session);
      }

      if (priceFrom != null && Objects.equals(priceFrom, spot)) {
         priceFrom = null;
      }

      return priceFrom;
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

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
}
