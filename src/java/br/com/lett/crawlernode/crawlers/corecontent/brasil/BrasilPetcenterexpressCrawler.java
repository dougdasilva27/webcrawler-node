package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
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
import models.AdvancedRatingReview;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class BrasilPetcenterexpressCrawler extends Crawler {

   private static final String SELLER_NAME = "Petcenter Express Brasil";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());


   public BrasilPetcenterexpressCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
   }

   @Override
   protected Object fetch() {
      // We append this parameter for load all coments to capture rating and reviews
      String url = session.getOriginalURL().contains("?") ? session.getOriginalURL() + "&comtodos=s" : session.getOriginalURL() + "?comtodos=s";

      Request request = RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .build();

      return Jsoup.parse(this.dataFetcher.get(session, request).getBody());
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();
      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, ".codProduto span", true);
         String internalPid = internalId;
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "#produto-nome", true);
         boolean available = !doc.select(".column.btnComprar .btn-comprar.action ").isEmpty(); // when this crawler was remade, there was no product that was unavailable
         //hasn't category
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#galleypc .car-gallery.thumbnails.slick-dotted.mobile-hide a", Arrays.asList("data-standard"), "https", "static.petcenterexpress.com.br");
         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, "#galleypc .car-gallery.thumbnails.slick-dotted.mobile-hide a", Arrays.asList("data-standard"), "https", "static.petcenterexpress.com.br", primaryImage);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".ui.grid.one.column"));
         RatingsReviews rating = scrapRatingReviews(doc);
         Offers offers = available ? scrapOffers(doc) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setOffers(offers)
            .setRatingReviews(rating)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      //site hasn't sale

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .build());


      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#variacao-preco span", null, false, ',', session);
      //site hasn't old price
      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
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


   private boolean isProductPage(Document doc) {
      return !doc.select(".row.detalhes.produto").isEmpty();
   }

   private RatingsReviews scrapRatingReviews(Document doc) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalNumOfEvaluations = doc.select(".customer-comment .comentarios-score-lista").size();
      Integer totalWrittenReviews = totalNumOfEvaluations;
      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(doc);
      Double avgRating = CrawlerUtils.extractRatingAverageFromAdvancedRatingReview(advancedRatingReview);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalWrittenReviews);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);

      return ratingReviews;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(Document doc) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      Elements reviews = doc.select(".customer-comment .comentarios-score-lista");

      for (Element review : reviews) {
         Integer val = review.select(".cheia").size();

         switch (val) {
            case 1:
               star1 += 1;
               break;
            case 2:
               star2 += 1;
               break;
            case 3:
               star3 += 1;
               break;
            case 4:
               star4 += 1;
               break;
            case 5:
               star5 += 1;
               break;
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
}
