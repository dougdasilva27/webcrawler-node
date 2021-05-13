package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.MathUtils;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BrasilDivvinoCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "Divvino";
   private static final List<Card> cards = Arrays.asList(Card.AMEX, Card.MASTERCARD, Card.VISA, Card.ELO, Card.HIPERCARD);

   public BrasilDivvinoCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document document) throws Exception {
      List<Product> products = new ArrayList<>();

      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(document, "[itemprop=\"sku\"]", "content");

      if (internalId != null) {

         String name = CrawlerUtils.scrapStringSimpleInfo(document, ".product_title", true);
         CategoryCollection categories = CrawlerUtils.crawlCategories(document, ".breadcrumbs_wrapper ul li", true);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(document, ".img_product_container a img", Collections.singletonList("src"), "https:", "statics.divvino.com.br");

         String description = CrawlerUtils.scrapSimpleDescription(document, Collections.singletonList("[itemprop=\"description\"]"));
         boolean available = document.select("#backInStockForm").isEmpty();
         Offers offers = available ? scrapOffers(document) : new Offers();

         RatingsReviews ratingsReviews = scrapRatingReviews(document);

         products.add(ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setDescription(description)
            .setOffers(offers)
            .setRatingReviews(ratingsReviews)
            .build());
      }

      return products;
   }

   private RatingsReviews scrapRatingReviews(Document document) {

      RatingsReviews ratingsReviews = new RatingsReviews();

      Double average = null;
      Integer totalReviews = null;

      String review = CrawlerUtils.scrapStringSimpleInfo(document,"#headerComment > div.row div.row p",true);
      String[] array = review != null ? review.split("/") : new String[0];

       if(array.length>1){
          average = MathUtils.parseDoubleWithDot(array[0]);
          String total = CommonMethods.substring(array[1],"(",")",true);
          totalReviews = MathUtils.parseInt(total);
       }

       ratingsReviews.setDate(session.getDate());
       ratingsReviews.setTotalRating(totalReviews);
       ratingsReviews.setAverageOverallRating(average);

      return ratingsReviews;
   }

   private Offers scrapOffers(Document document) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();

      Pricing pricing = scrapPricing(document);

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

   private Pricing scrapPricing(Document document) throws MalformedPricingException {



      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(document, "#loadProductPrice  .product_price_new", "content", false, '.', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(document, "#loadProductPrice > div > span", null, true, ',', session);

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(BankSlip.BankSlipBuilder.create()
            .setFinalPrice(spotlightPrice)
            .build())
         .build();

   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {

      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentPrice(spotlightPrice)
         .setInstallmentNumber(1)
         .build());
      for (Card card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card.toString())
            .setIsShopCard(false)
            .setInstallments(installments)
            .build());
      }
      return creditCards;

   }
}
