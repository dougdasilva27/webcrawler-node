package br.com.lett.crawlernode.crawlers.corecontent.espana;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.google.common.collect.Sets;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer.OfferBuilder;
import models.Offers;
import models.RatingsReviews;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;

public class EspanaDiaCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "Dia";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public EspanaDiaCrawler(Session session) {
      super(session);
   }


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();
      if (doc.selectFirst("#productDetailUpdateable") != null) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".prod_add_to_cart .product-initialAddToCart-button", "data-productcode");
         String internalPid = internalId;
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".block1 h1", false);
         boolean available = !doc.select(".product-info .in-stock").isEmpty();
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "#breadcrumb li h3 [itemprop=\"name\"]", true);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".span-10 #primary_image .productZoom", Arrays.asList("href"), "https",
               "www.dia.es");
         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc,
               "#carousel_alternate .item a img",
               Arrays.asList("src"), "https", "www.dia.es", primaryImage);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".short-description .std", ".tab-content .std"));
         Offers offers = scrapOffer(doc);
         RatingsReviews ratingReviews = scrapRatingReviews(doc, internalId, categories);

         // Creating the product

         Product product = ProductBuilder.create()
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
               .setOffers(offers)
               .setRatingReviews(ratingReviews)

               .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }


   private Offers scrapOffer(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = new ArrayList<>();

      offers.add(OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(SELLER_FULL_NAME)
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .setSales(sales)
            .build());

      return offers;

   }


   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".span-11 .big-price", null, false, ',', session);
      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);

      return PricingBuilder.create()
            .setSpotlightPrice(spotlightPrice)
            .setCreditCards(creditCards)
            .build();


   }

   private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      if (installments.getInstallments().isEmpty()) {
         installments.add(InstallmentBuilder.create()
               .setInstallmentNumber(1)
               .setInstallmentPrice(spotlightPrice)
               .build());
      }

      for (String card : cards) {
         creditCards.add(CreditCardBuilder.create()
               .setBrand(card)
               .setInstallments(installments)
               .setIsShopCard(false)
               .build());
      }

      return creditCards;
   }


   private Document getAllRating(String internalId, CategoryCollection categorias) {

      String url = "https://www.dia.es/compra-online/productos/" + categorias + "/p/" + internalId + "/reviewhtml/all";

      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
      Document docRating = Jsoup.parse(this.dataFetcher.get(session, request).getBody());

      return docRating;
   }


   private RatingsReviews scrapRatingReviews(Document doc, String internalId, CategoryCollection categorias) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Document docRating = getAllRating(internalId, categorias);

      System.err.println(docRating);
      Integer totalNumOfEvaluations = CrawlerUtils.scrapIntegerFromHtml(doc, ".prod_review #based_on_reviews", false, 0);
      Double avgRating = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".prod_review .rateyo-readonly", "data-rating", false, '.', session);
      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(docRating, doc);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);

      return ratingReviews;
   }


   private AdvancedRatingReview scrapAdvancedRatingReview(Document docRating, Document doc) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      Elements reviews = docRating.select(".review_detail");

      for (Element review : reviews) {

         Element elementStarNumber = review.selectFirst(".rateyo-readonly");

         if (elementStarNumber != null) {

            String dataRating = elementStarNumber.attr("data-rating");
            Integer starNumber = dataRating != null ? MathUtils.parseInt(dataRating) : null;

            switch (starNumber) {
               case 50:
                  star5 += 1;
                  break;
               case 40:
                  star4 += 1;
                  break;
               case 30:
                  star3 += 1;
                  break;
               case 20:
                  star2 += 1;
                  break;
               case 10:
                  star1 += 1;
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


}