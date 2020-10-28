package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import com.google.common.collect.Sets;
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import br.com.lett.crawlernode.util.Pair;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer;
import models.Offer.OfferBuilder;
import models.Offers;
import models.RatingsReviews;
import models.pricing.BankSlip;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;

/**
 * Date: 08/10/2018
 * 
 * @author Gabriel Dornelas
 *
 */
public class Mercadolivre3pCrawler {

   private String mainSellerNameLower;
   private Session session;
   private DataFetcher dataFetcher;
   private Logger logger;
   protected boolean allow3PSellers = false;
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString());

   protected Mercadolivre3pCrawler(Session session, DataFetcher dataFetcher, String mainSellerNameLower, boolean allow3PSellers, Logger logger) {
      this.session = session;
      this.dataFetcher = dataFetcher;
      this.mainSellerNameLower = mainSellerNameLower;
      this.logger = logger;
      this.allow3PSellers = allow3PSellers;
   }

   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         boolean availableToBuy = !doc.select(".andes-button--filled, .andes-button__content").isEmpty();
         Offers offers = availableToBuy ? scrapOffers(doc) : new Offers();
         boolean mustAddProduct = checkIfMustScrapProduct(offers);

         if (mustAddProduct) {
            JSONObject jsonInfo = CrawlerUtils.selectJsonFromHtml(doc, "script[type=\"application/ld+json\"]", "", null, false, false);
            String internalPid = jsonInfo.optString("productID");
            String internalId = jsonInfo.optString("sku");

            String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.ui-pdp-title", true);
            CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".andes-breadcrumb__item a");
            String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "figure.ui-pdp-gallery__figure img", Arrays.asList("data-zoom", "src"), "https:",
                  "http2.mlstatic.com");
            String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "figure.ui-pdp-gallery__figure img", Arrays.asList("data-zoom", "src"), "https:",
                  "http2.mlstatic.com", primaryImage);
            String description =
                  CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".ui-pdp-features", ".ui-pdp-description", ".ui-pdp-specs"));

            RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();
            ratingReviewsCollection.addRatingReviews(crawlRating(doc, internalPid, internalId));
            RatingsReviews ratingReviews = ratingReviewsCollection.getRatingReviews(internalId);


            // Creating the product
            Product product = ProductBuilder.create()
                  .setUrl(session.getOriginalURL())
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setCategory1(categories.getCategory(0))
                  .setCategory2(categories.getCategory(1))
                  .setCategory3(categories.getCategory(2))
                  .setPrimaryImage(primaryImage != null ? primaryImage.replace(".webp", ".jpg") : null)
                  .setSecondaryImages(secondaryImages != null ? secondaryImages.replace(".webp", ".jpg") : null)
                  .setDescription(description)
                  .setRatingReviews(ratingReviews)
                  .setOffers(offers)
                  .build();

            products.add(product);
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private boolean isProductPage(Document doc) {
      return !doc.select("h1.ui-pdp-title").isEmpty();
   }

   private boolean checkIfMustScrapProduct(Offers offers) {
      boolean mustAddProduct = this.allow3PSellers;
      if (!allow3PSellers) {
         List<Offer> offersList = offers.getOffersList();

         for (Offer offer : offersList) {
            if (offer.getIsMainRetailer()) {
               mustAddProduct = true;
               break;
            }
         }
      }

      return offers.isEmpty() || mustAddProduct;
   }

   private RatingsReviews crawlRating(Document doc, String internalPid, String internalId) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalNumOfEvaluations = CrawlerUtils.scrapIntegerFromHtml(doc, ".ui-pdp-reviews__rating__summary__label", true, 0);
      Double avgRating = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".ui-pdp-reviews__rating__summary__average", null, true, '.', session);
      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(internalId);

      ratingReviews.setInternalId(internalId);
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating == null ? 0d : avgRating);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);

      return ratingReviews;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(String internalId) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      Document docRating = acessHtmlWithAdvanedRating(internalId);
      Elements reviews = docRating.select(".reviews-rating .review-rating-row.is-rated");

      for (Element review : reviews) {

         Element elementStarNumber = review.selectFirst(".review-rating-label");

         if (elementStarNumber != null) {
            String stringStarNumber = elementStarNumber.text().replaceAll("[^0-9]", "").trim();
            Integer numberOfStars = !stringStarNumber.isEmpty() ? Integer.parseInt(stringStarNumber) : 0;

            Element elementVoteNumber = review.selectFirst(".review-rating-total");

            if (elementVoteNumber != null) {

               String vN = elementVoteNumber.text().replaceAll("[^0-9]", "").trim();
               Integer numberOfVotes = !vN.isEmpty() ? Integer.parseInt(vN) : 0;

               switch (numberOfStars) {
                  case 5:
                     star5 = numberOfVotes;
                     break;
                  case 4:
                     star4 = numberOfVotes;
                     break;
                  case 3:
                     star3 = numberOfVotes;
                     break;
                  case 2:
                     star2 = numberOfVotes;
                     break;
                  case 1:
                     star1 = numberOfVotes;
                     break;
                  default:
                     break;
               }
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

   private Document acessHtmlWithAdvanedRating(String internalId) {
      StringBuilder url = new StringBuilder();
      url.append("https://produto.mercadolivre.com.br/noindex/catalog/reviews/")
            .append(internalId)
            .append("?noIndex=true")
            .append("&contextual=true")
            .append("&access=view_all")
            .append("&quantity=1");

      Map<String, String> headers = new HashMap<>();
      headers.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.87 Safari/537.36");
      headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");

      Request request = RequestBuilder.create().setUrl(url.toString()).build();
      String response = dataFetcher.get(session, request).getBody().trim();

      return Jsoup.parse(response);

   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      String sellerFullName = CrawlerUtils.scrapStringSimpleInfo(doc, ".ui-pdp-seller__header__title a", false);

      if (sellerFullName == null) {
         sellerFullName = CrawlerUtils.scrapStringSimpleInfo(doc, ".ui-pdp-seller__header__title", false);
      }

      boolean hasMainOffer = false;

      if (sellerFullName != null && !sellerFullName.isEmpty()) {
         Pricing pricing = scrapPricing(doc);
         List<String> sales = scrapSales(doc);

         offers.add(OfferBuilder.create()
               .setUseSlugNameAsInternalSellerId(true)
               .setSellerFullName(sellerFullName)
               .setMainPagePosition(1)
               .setIsBuybox(false)
               .setIsMainRetailer(mainSellerNameLower.equalsIgnoreCase(sellerFullName))
               .setPricing(pricing)
               .setSales(sales)
               .build());

         hasMainOffer = true;
      }

      scrapSellersPage(offers, doc, hasMainOffer);

      return offers;
   }

   private void scrapSellersPage(Offers offers, Document doc, boolean hasMainOffer) throws OfferException, MalformedPricingException {
      String sellersPageUrl = CrawlerUtils.scrapUrl(doc, ".ui-pdp-other-sellers__link", "href", "https", "www.mercadolivre.com.br");
      if (sellersPageUrl != null) {
         String nextUrl = sellersPageUrl;

         int sellersPagePosition = 1;
         boolean mainOfferFound = false;

         do {
            Request request = RequestBuilder.create()
                  .setUrl(nextUrl)
                  .build();

            Document sellersHtml = Jsoup.parse(this.dataFetcher.get(session, request).getBody());
            nextUrl = CrawlerUtils.scrapUrl(sellersHtml, ".andes-pagination__button--next:not(.andes-pagination__button--disabled) a", "href", "https", "www.mercadolivre.com.br");

            Elements offersElements = sellersHtml.select("form.ui-pdp-buybox");
            if (!offersElements.isEmpty()) {
               for (Element e : offersElements) {
                  String sellerName = CrawlerUtils.scrapStringSimpleInfo(e, ".ui-pdp-action-modal__link", false);
                  if (hasMainOffer && sellerName != null && !mainOfferFound && offers.containsSeller(sellerName)) {
                     Offer offerMainPage = offers.getSellerByName(sellerName);
                     offerMainPage.setSellersPagePosition(sellersPagePosition);
                     offerMainPage.setIsBuybox(true);

                     mainOfferFound = true;
                  } else {
                     Pricing pricing = scrapPricing(e);
                     List<String> sales = scrapSales(e);

                     offers.add(OfferBuilder.create()
                           .setUseSlugNameAsInternalSellerId(true)
                           .setSellerFullName(sellerName)
                           .setSellersPagePosition(sellersPagePosition)
                           .setIsBuybox(true)
                           .setIsMainRetailer(mainSellerNameLower.equalsIgnoreCase(sellerName))
                           .setPricing(pricing)
                           .setSales(sales)
                           .build());
                  }

                  sellersPagePosition++;
               }
            } else {
               break;
            }

         } while (nextUrl != null);
      } else {
         if (offers.isEmpty()) {
            Pricing pricing = scrapPricing(doc);
            List<String> sales = scrapSales(doc);
            offers.add(OfferBuilder.create()
                  .setUseSlugNameAsInternalSellerId(true)
                  .setSellerFullName(mainSellerNameLower)
                  .setIsMainRetailer(true)
                  .setIsBuybox(true)
                  .setPricing(pricing)
                  .setSales(sales)
                  .build());
         }
      }
   }

   private List<String> scrapSales(Element doc) {
      List<String> sales = new ArrayList<>();

      Element salesOneElement = doc.selectFirst(".ui-pdp-price__second-line__label");
      String firstSales = salesOneElement != null ? salesOneElement.text() : null;

      if (firstSales != null && !firstSales.isEmpty()) {
         sales.add(firstSales);
      }

      return sales;
   }

   private Pricing scrapPricing(Element doc) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "del.price-tag", null, false, ',', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "span.price-tag", null, false, ',', session);
      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);
      BankSlip bankTicket = BankSlipBuilder.create()
            .setFinalPrice(spotlightPrice)
            .build();

      return PricingBuilder.create()
            .setPriceFrom(priceFrom)
            .setSpotlightPrice(spotlightPrice)
            .setCreditCards(creditCards)
            .setBankSlip(bankTicket)
            .build();
   }


   private CreditCards scrapCreditCards(Element doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = scrapInstallments(doc);
      installments.add(InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());

      for (String card : cards) {
         creditCards.add(CreditCardBuilder.create()
               .setBrand(card)
               .setInstallments(installments)
               .setIsShopCard(false)
               .build());
      }

      return creditCards;
   }

   public Installments scrapInstallments(Element doc, String selector) throws MalformedPricingException {
      Installments installments = new Installments();

      Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(selector, doc, false);
      if (!pair.isAnyValueNull()) {
         installments.add(InstallmentBuilder.create()
               .setInstallmentNumber(pair.getFirst())
               .setInstallmentPrice(MathUtils.normalizeTwoDecimalPlaces(((Float) pair.getSecond()).doubleValue()))
               .build());
      }

      return installments;
   }

   public Installments scrapInstallments(Element doc) throws MalformedPricingException {

      Installments installments = scrapInstallments(doc, ".ui-pdp-payment--md .ui-pdp-media__title");

      if (installments == null || installments.getInstallments().isEmpty()) {
         return scrapInstallmentsV2(doc);
      }
      return installments;
   }

   public Installments scrapInstallmentsV2(Element doc) throws MalformedPricingException {

      return scrapInstallments(doc, ".ui-pdp-container__row--payment-summary .ui-pdp-media__title");
   }
}
