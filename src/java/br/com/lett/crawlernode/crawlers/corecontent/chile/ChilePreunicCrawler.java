package br.com.lett.crawlernode.crawlers.corecontent.chile;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
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
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ChilePreunicCrawler extends Crawler {

   public ChilePreunicCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, "span#product__sku", true).replace("SKU: ", "");
         String name = scrapNameWithBrand(doc);
         String primaryImage = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "div#main-image img", "src");
         List<String> secondaryImages = scrapImages(doc);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Collections.singletonList("div.description-tabs"));
         CategoryCollection categories = scrapCategories(doc);

         //Cannot find any unnavailable product
         boolean available = true;
         Offers offers = available ? scrapOffers(doc) : new Offers();

         //In this website, the reviews contain only 2 options: thumbs up or thumbs down.
         //So, we can capture the total ratings, but I can't  think in a way to capture the average rating in this format
         //RatingsReviews ratings = crawlRating(doc);

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalPid)
            .setInternalPid(internalPid)
            .setName(name)
            .setOffers(offers)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setCategories(categories)
//            .setRatingReviews(ratings)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private String scrapNameWithBrand(Document doc){
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h3.product__title", true);
      String brand = CrawlerUtils.scrapStringSimpleInfo(doc, "#product-info .product__brand", true);

      if ( name != null && brand != null){
         return name + " - " + brand;
      }

      return name;

   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("div#product-info") != null;
   }

   private List<String> scrapImages(Document doc) {
      List<String> imgs = new ArrayList<>();

      Elements imgList = doc.select("div.product-details.image-box a.active.inner-product-box");
      for (Element el : imgList) {
         String url = el.attr("href");

         if (url != null) {
            imgs.add(url);
         }
      }

      return imgs;
   }

   private CategoryCollection scrapCategories(Document doc) {
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "ol.breadcrumb li span[itemprop=name]", true);

      if(categories.getCategory(0).equals("Productos")){
         categories.remove(0);
      }

      return categories;
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      List<String> sales = new ArrayList<>();

      Pricing pricing = scrapPricing(doc);
      sales.add(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setPricing(pricing)
         .setSales(sales)
         .setSellerFullName("preunic")
         .setIsMainRetailer(true)
         .setUseSlugNameAsInternalSellerId(true)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "span.offer-price", null, true, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "span.original-price span", null, true, ',', session);

      if(spotlightPrice == null){
         spotlightPrice = priceFrom;
         priceFrom = null;
      }

      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);
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

   private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      if (doc.selectFirst("div.discount-price-preunic") != null) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(Card.SHOP_CARD.toString())
            .setInstallments(scrapShopcardPrice(doc))
            .setIsShopCard(true)
            .build());
      }
      return creditCards;
   }

   private Installments scrapShopcardPrice(Document doc) throws MalformedPricingException {
      Installments installments = new Installments();
      Double shopcardPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "div.discount-price-preunic", null, true, ',', session);

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(shopcardPrice)
         .build());

      return installments;
   }

//   private RatingsReviews crawlRating(Document doc) {
//      RatingsReviews ratingReviews = new RatingsReviews();
//      ratingReviews.setDate(session.getDate());
//
//      ratingReviews.setTotalRating(CrawlerUtils.scrapIntegerFromHtml(doc, "div.details-qualification span.title rating", true, 0));
//      ratingReviews.setTotalWrittenReviews(CrawlerUtils.scrapIntegerFromHtml(doc, "div.details-qualification span.title rating", true, 0));
//
//      return ratingReviews;
//   }
}
