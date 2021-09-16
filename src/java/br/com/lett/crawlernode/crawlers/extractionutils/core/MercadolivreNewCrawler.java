package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.*;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.*;
import com.google.common.collect.Sets;
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
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;

/**
 * Date: 08/10/2018
 *
 * @author Gabriel Dornelas
 */
public class MercadolivreNewCrawler {

   private String mainSellerNameLower;
   List<String> sellersVariations;
   private Session session;
   private DataFetcher dataFetcher;
   private Logger logger;
   protected boolean allow3PSellers;
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString());

   protected MercadolivreNewCrawler(Session session, DataFetcher dataFetcher, String mainSellerNameLower, boolean allow3PSellers, Logger logger, List<String> sellersVariations) {
      this.session = session;
      this.dataFetcher = dataFetcher;
      this.mainSellerNameLower = mainSellerNameLower;
      this.logger = logger;
      this.allow3PSellers = allow3PSellers;
      this.sellersVariations = sellersVariations;
   }

   public Product extractInformation(Document doc,
                                     RatingsReviews ratingReviews
   ) throws OfferException, MalformedPricingException, MalformedProductException {
      Product product = null;
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      boolean availableToBuy = !doc.select(".andes-button--filled, .andes-button__content").isEmpty();
      Offers offers = availableToBuy ? scrapOffers(doc) : new Offers();
      boolean mustAddProductUnavailable = !availableToBuy && checkIfMustScrapProductUnavailable(doc);
      boolean mustAddProduct = availableToBuy && checkIfMustScrapProduct(offers);

      if (mustAddProduct || mustAddProductUnavailable) {
         JSONObject jsonInfo = CrawlerUtils.selectJsonFromHtml(doc, "script[type=\"application/ld+json\"]", "", null, false, false);

         String internalPid = jsonInfo.optString("productID");
         String internalId;
         Element variationElement = doc.selectFirst("input[name='variation']");
         if (variationElement != null && !doc.select(".ui-pdp-variations .ui-pdp-variations__picker:not(.ui-pdp-variations__picker-single) a").isEmpty() || !doc.select(".andes-dropdown__popover ul li").isEmpty()) {
            internalId = internalPid + '_' + variationElement.attr("value");
         } else {
            internalId = jsonInfo.optString("sku");
         }

         String name = scrapName(doc);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".andes-breadcrumb__item a");
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "figure.ui-pdp-gallery__figure img", Arrays.asList("data-zoom", "src"), "https:",
            "http2.mlstatic.com");
         List<String> secondaryImages = crawlImages(primaryImage, doc);
         String description =
            CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".ui-pdp-features", ".ui-pdp-description", ".ui-pdp-specs"));

         RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();
         ratingReviewsCollection.addRatingReviews(crawlRating(doc, internalId));
         ratingReviews = Objects.isNull(ratingReviews) ? ratingReviewsCollection.getRatingReviews(internalId) : ratingReviews;

         product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage != null ? primaryImage.replace(".webp", ".jpg") : null)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setRatingReviews(ratingReviews)
            .setOffers(offers)
            .build();
      }

      return product;
   }

   private String scrapName(Document doc) {
      String productName = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.ui-pdp-title", true);
      StringBuilder name = new StringBuilder();
      name.append(productName);

      Elements variationsElements = doc.select(".ui-pdp-variations__selected-label");

      if (!variationsElements.isEmpty()) {

         for (Element e : variationsElements) {
            name.append(" ").append(e.ownText().trim());
         }

      } else if (!doc.select(".andes-dropdown__popover ul li").isEmpty()) {

         name.append(" ").append(CrawlerUtils.scrapStringSimpleInfo(doc, ".ui-pdp-dropdown-selector__item--label", true));

      }


      return name.toString();
   }

   private List<String> crawlImages(String primaryImage, Document doc) {
      List<String> images = CrawlerUtils.scrapSecondaryImages(doc, "figure.ui-pdp-gallery__figure img", Arrays.asList("data-zoom", "src"), "htps", "http2.mlstatic.com", primaryImage);
      List<String> secondaryImages = new ArrayList<>();
      if (!images.isEmpty()) {
         for (String secondaryImage : images) {
            secondaryImages.add(secondaryImage.replace(".webp", ".jpg"));
         }
      }
      return secondaryImages;
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

      return mustAddProduct;
   }

   private boolean checkIfMustScrapProductUnavailable(Document doc) {
      boolean mustAddProductUnavailable = this.allow3PSellers;
      if (!allow3PSellers) {
         mustAddProductUnavailable = scrapSeller(doc) != null && scrapSeller(doc).equalsIgnoreCase(mainSellerNameLower);
      }

      return mustAddProductUnavailable;
   }

   private RatingsReviews crawlRating(Document doc, String internalId) {
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

   public String scrapSeller(Document doc) {
      String sellerFullName = CrawlerUtils.scrapStringSimpleInfo(doc, ".ui-pdp-seller__header__title", false);

      if (sellerFullName == null || sellerFullName.equalsIgnoreCase("Vendido por") || sellerFullName.equalsIgnoreCase("Loja oficial")) {
         sellerFullName = CrawlerUtils.scrapStringSimpleInfo(doc, "a.ui-pdp-action-modal__link span", false);

         if (sellerFullName == null || sellerFullName.contains("sem juros")) {
            String url = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".ui-pdp-container__row--seller-info a", "href");

            if (url != null) {
               try {
                  sellerFullName = URLDecoder.decode(CommonMethods.getLast(url.split("\\?")[0].split("/")), "UTF-8");
               } catch (UnsupportedEncodingException e) {
                  Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
               }
            }
         }
      }
      return sellerFullName;
   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      String sellerFullName = scrapSeller(doc);

      boolean hasMainOffer = false;
      boolean isMainRetailer;


      if (sellersVariations == null) {
         isMainRetailer = mainSellerNameLower.equalsIgnoreCase(sellerFullName);
      } else {
         isMainRetailer = isMainRetaler(sellerFullName);

      }

      if (sellerFullName != null && !sellerFullName.isEmpty()) {
         Pricing pricing = scrapPricing(doc);
         List<String> sales = scrapSales(doc);

         offers.add(OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(sellerFullName)
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(isMainRetailer)
            .setPricing(pricing)
            .setSales(sales)
            .build());

         hasMainOffer = true;
      }


      scrapSellersPage(offers, doc, hasMainOffer);

      return offers;
   }

   private boolean isMainRetaler(String sellerFullName) {
      boolean isMainRetailer = false;

      for (String sellerName : sellersVariations) {
         if (sellerName.equalsIgnoreCase(sellerFullName)) {
            isMainRetailer = true;
         }
      }
      return isMainRetailer;
   }

   private void scrapSellersPage(Offers offers, Document doc, boolean hasMainOffer) throws OfferException, MalformedPricingException {
      String sellersPageUrl = CrawlerUtils.scrapUrl(doc, ".ui-pdp-other-sellers__link", "href", "https", "www.mercadolivre.com.br");
      if (sellersPageUrl != null) {
         String nextUrl = sellersPageUrl;

         int sellersPagePosition = 1;
         boolean mainOfferFound = false;
         String spotlightSellerName = offers.size() > 0 ? offers.getOffersList().get(0).getSellerFullName() : "";

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
                  if (hasMainOffer && sellerName != null && !mainOfferFound && spotlightSellerName.equalsIgnoreCase(sellerName)) {
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
      Double spotlightPrice = findSpotlightPrice(doc);
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

   private Double findSpotlightPrice(Element doc) {
      Double price = CrawlerUtils.scrapDoublePriceFromHtml(doc, "span.price-tag meta", "content", false, '.', session);
      if (price == null) {
         price = CrawlerUtils.scrapDoublePriceFromHtml(doc, "div.ui-pdp-price span.price-tag-amount", null, false, ',', session);
      }
      return price;
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
