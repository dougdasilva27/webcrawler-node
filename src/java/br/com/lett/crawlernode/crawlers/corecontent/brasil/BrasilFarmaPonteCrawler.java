package br.com.lett.crawlernode.crawlers.corecontent.brasil;

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

import java.util.*;

public class BrasilFarmaPonteCrawler extends Crawler {

   private static  String SELLER_NAME = "Farma Ponte";

   public Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),Card.ELO.toString(),
      Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString(), Card.VISAELECTRON.toString());

   public BrasilFarmaPonteCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String productName = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-detail .name", false);
         String internalId = scrapInternalId(doc);
         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "meta[itemprop=\"sku\"]", "content");
         String description = CrawlerUtils.scrapStringSimpleInfo(doc, ".card-body", false);
         List<String> categories = CrawlerUtils.crawlCategories(doc, "#breadcrumb ul li a", true);
         String primaryImage = scrapLargeImage(CrawlerUtils.scrapSimplePrimaryImage(doc, ".big-image .zoom .img-lazy", List.of("data-src"), "https:", ""));
         List<String> secondaryImages = scrapSecondaryImages(doc, primaryImage);
         boolean isAvailable = doc.selectFirst(".purchase .btn.btn-checkout.btn-let-me-know") == null;
         Offers offers = isAvailable ? scrapOffers(doc) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(productName)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
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

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("#content-product") != null;
   }

   private String scrapInternalId(Document doc) {
      String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, ".mr-3", false);
      assert internalId != null;
      return internalId.replaceAll("Cód: ", "");
   }

   private String scrapLargeImage(String image) {
      return image.replaceAll("/(mini|small|medium|large)/", "/large/");
   }

   private List<String> scrapSecondaryImages(Document doc, String primaryImage) {
      List<String> imagesList = CrawlerUtils.scrapSecondaryImages(doc, ".thumbs .item img", List.of("data-src"),"https:", "", null);
      List<String> secondaryImages = new ArrayList<>();
      for (String image : imagesList) {
         image = scrapLargeImage(image);
         if (primaryImage == null || !primaryImage.equals(image)) {
            secondaryImages.add(image);
         }
      }
      return secondaryImages;
   }
   
   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
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

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-form .pix-price strong", null, false, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-form .unit-price span", null, false, ',', session);
      spotlightPrice = (spotlightPrice == null) ? priceFrom : spotlightPrice;
      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();
   }

   private CreditCards scrapCreditCards(Double price) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(price)
         .setFinalPrice(price)
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