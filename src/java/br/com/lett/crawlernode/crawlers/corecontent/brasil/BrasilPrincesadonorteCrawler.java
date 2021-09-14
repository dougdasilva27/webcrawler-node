package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Joao Pedro
 */
public class BrasilPrincesadonorteCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.princesadonorteonline.com.br/";
   private static final String SELLER_FULL_NAME = "Princesa do norte";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public BrasilPrincesadonorteCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.FETCHER);
   }


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {


         String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, "div[itemprop=\"sku\"]", true);
         String internalPid = internalId;
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".page-title span", true);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".description", ".additional-attributes"));
         JSONArray imagesArray = CrawlerUtils.crawlArrayImagesFromScriptMagento(doc);
         String primaryImage = CrawlerUtils.scrapPrimaryImageMagento(imagesArray);
         String secondaryImage = CrawlerUtils.scrapSecondaryImagesMagento(imagesArray, primaryImage);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".items li", false);
         Offers offers = scrapOffer(doc);


         RatingsReviews ratingsReviews = scrapRatingReviews(doc);

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImage)
            .setDescription(description)
            .setRatingReviews(ratingsReviews)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".product-info-main") != null;
   }

   private Offers scrapOffer(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);

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
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".special-price .price", null, false, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".old-price .price", null, false, ',', session);

      if (spotlightPrice == null) {
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price", null, false, ',', session);
      }

      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
   }


   private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = scrapInstallments(doc);
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

   public Installments scrapInstallments(Document doc) throws MalformedPricingException {
      Installments installments = new Installments();


      Elements elements = doc.select("ul.installments-options li");

      for (Element element : elements) {

         String installmentsText = element.text();

         final Pattern pattern = Pattern.compile("(\\d)x.*R.(\\d+,\\d*)");
         final Matcher matcher = pattern.matcher(installmentsText);

         if (matcher.find()) {

            Integer installment = MathUtils.parseInt(matcher.group(1));
            Double value = MathUtils.parseDoubleWithComma(matcher.group(2));

            installments.add(Installment.InstallmentBuilder.create()
               .setInstallmentNumber(installment)
               .setInstallmentPrice(value)
               .build());

         }


      }
      return installments;
   }

   private RatingsReviews scrapRatingReviews(Document doc) {

      RatingsReviews ratingsReviews = new RatingsReviews();

      int totalReviews = doc.select(".ratings-table").size();
      double avgRating = 0.0;

      Double percentageRating = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-shop .rating", "style", true, '.', session);

      if (percentageRating != null) {
         avgRating = percentageRating / 100 * 5;
      }

      ratingsReviews.setTotalRating(totalReviews);
      ratingsReviews.setTotalWrittenReviews(totalReviews);
      ratingsReviews.setAverageOverallRating(avgRating);
      ratingsReviews.setAdvancedRatingReview(scrapAdvancedRatingReview(doc));

      return ratingsReviews;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(Document doc) {

      AdvancedRatingReview advancedRatingReview = new AdvancedRatingReview();

      Elements ratings = doc.select(".ratings-table");

      int stars1 = 0;
      int stars2 = 0;
      int stars3 = 0;
      int stars4 = 0;
      int stars5 = 0;

      for (Element e : ratings) {

         int stars = 0;
         Double starsformatted = CrawlerUtils.scrapDoublePriceFromHtml(e, ".ratings-table tr:first-child .rating", "style", true, '.', session);

         if (starsformatted != null) {
            stars = (int) (starsformatted / 100 * 5);
         }

         switch (stars) {
            case 1:
               stars1++;
               break;
            case 2:
               stars2++;
               break;
            case 3:
               stars3++;
               break;
            case 4:
               stars4++;
               break;
            case 5:
               stars5++;
               break;
            default:
         }
      }

      advancedRatingReview.setTotalStar1(stars1);
      advancedRatingReview.setTotalStar2(stars2);
      advancedRatingReview.setTotalStar3(stars3);
      advancedRatingReview.setTotalStar4(stars4);
      advancedRatingReview.setTotalStar5(stars5);

      return advancedRatingReview;

   }

}
