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
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

      boolean availableToBuy = isAvailable(doc);
      Offers offers = availableToBuy ? scrapOffers(doc) : new Offers();
      boolean mustAddProductUnavailable = !availableToBuy && checkIfMustScrapProductUnavailable(doc);

      if (!offers.isEmpty() || mustAddProductUnavailable) {

         JSONObject initialState = selectJsonFromHtml(doc);
         JSONObject schema = initialState != null ? JSONUtils.getValueRecursive(initialState, "schema.0", JSONObject.class) : null;
         if (schema != null) {
            String internalPid = schema.optString("productID");
            String internalId;
            Element variationElement = doc.selectFirst("input[name='variation']");
            if (variationElement != null && (!doc.select(".ui-pdp-variations .ui-pdp-variations__picker:not(.ui-pdp-variations__picker-single) a").isEmpty() || !doc.select(".andes-dropdown__popover ul li").isEmpty())) {
               internalId = internalPid + '_' + variationElement.attr("value");
            } else {
               internalId = schema.optString("sku");
            }

            String name = scrapName(doc);
            CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".andes-breadcrumb__item a");
            String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "figure.ui-pdp-gallery__figure img", Arrays.asList("data-zoom", "src"), "https:",
               "http2.mlstatic.com");
            List<String> secondaryImages = crawlImages(primaryImage, doc);
            String description =
               CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".ui-pdp-features", ".ui-pdp-description__content", ".ui-pdp-specs"));

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
      }
      return product;
   }

   private boolean isAvailable(Document doc) {
      boolean availableToBuy = !doc.select(".andes-button--filled, .andes-button__content").isEmpty();

      String unavailable = CrawlerUtils.scrapStringSimpleInfo(doc, ".ui-pdp-shipping-message__text", true);

      if (unavailable != null && unavailable.contains("indisponível")) {
         availableToBuy = false;
      }

      String adStatus = CrawlerUtils.scrapStringSimpleInfo(doc, ".andes-message__text.andes-message__text--warning", true);

      if (adStatus != null && adStatus.contains("Anúncio pausado")) {
         availableToBuy = false;
      }

      String unavailableAddress = CrawlerUtils.scrapStringSimpleInfo(doc, ".ui-vip-shipping-message__text", false);

      if (unavailableAddress != null && unavailableAddress.contains("Este anúncio está indisponível para seu endereço.")) {
         availableToBuy = false;
      }

      return availableToBuy;
   }

   private String scrapName(Document doc) {
      String productName = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.ui-pdp-title", true);
      StringBuilder name = new StringBuilder();
      name.append(productName);

      Elements variationsElements = doc.select(".ui-pdp-variations__selected-label");

      if (!variationsElements.isEmpty()) {

         for (Element e : variationsElements) {
            String variation = CrawlerUtils.scrapStringSimpleInfo(e, ".ui-pdp-variations__selected-text", true);
            if (variation != null && !productName.replace(" ", "").contains(variation)) {
               name.append(" - ").append(variation);
            }
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

   private boolean checkIfMustScrapProductUnavailable(Document doc) {
      boolean mustAddProductUnavailable = this.allow3PSellers;
      String seller = scrapSeller(doc);
      if (!allow3PSellers) {
         if (!mainSellerNameLower.isEmpty()) {
            mustAddProductUnavailable = seller != null && seller.equalsIgnoreCase(mainSellerNameLower);
         } else {
            for (String sellerName : sellersVariations) {
               if (sellerName.equalsIgnoreCase(seller)) {
                  mustAddProductUnavailable = true;
                  break;
               }
            }
         }
      }
      return mustAddProductUnavailable;
   }

   private RatingsReviews crawlRating(Document doc, String internalId) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalNumOfEvaluations = CrawlerUtils.scrapIntegerFromHtml(doc, ".ui-review-capability__rating__label", true, 0);
      Double avgRating = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".ui-review-capability__rating__average", null, true, '.', session);
      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(doc, totalNumOfEvaluations);

      ratingReviews.setInternalId(internalId);
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating == null ? 0d : avgRating);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);

      return ratingReviews;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(Document doc, Integer totalNumOfEvaluations) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      if (totalNumOfEvaluations > 0) {
         for (Element starElement : doc.select(".ui-review-capability-rating > li")) {
            Integer star = CrawlerUtils.scrapIntegerFromHtml(starElement, ".ui-review-capability-rating__level__value-container > span", true, 0);
            Double selectorPercentage = CrawlerUtils.scrapDoublePriceFromHtml(starElement, ".ui-review-capability-rating__level__progress-bar__fill-background", "style", false, '.', session);
            Integer total = Math.toIntExact(selectorPercentage > 0 ? Math.round((totalNumOfEvaluations * (selectorPercentage / 100f))) : 0);

            switch (star) {
               case 5:
                  star5 = total;
                  break;
               case 4:
                  star4 = total;
                  break;
               case 3:
                  star3 = total;
               case 2:
                  star2 = total;
               case 1:
                  star1 = total;
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

   public String scrapSeller(Document doc) {
      String sellerFullName = CrawlerUtils.scrapStringSimpleInfo(doc, ".ui-pdp-seller__header__title", false);

      if (sellerFullName == null || sellerFullName.equalsIgnoreCase("Vendido por") || sellerFullName.equalsIgnoreCase("Loja oficial")) {
         sellerFullName = CrawlerUtils.scrapStringSimpleInfo(doc, "a.ui-pdp-action-modal__link span", false);

         if (sellerFullName == null) {
            sellerFullName = CrawlerUtils.scrapStringSimpleInfo(doc, ".ui-pdp-reputation-title-link", false);
         }

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

      isMainRetailer = checkIsMainRetailer(sellerFullName);
      Double priceInMainOffer = findSpotlightPrice(doc);

      if ((isMainRetailer || allow3PSellers) && priceInMainOffer != 0.0) {
         Pricing pricing = scrapPricing(doc, priceInMainOffer);
         List<String> sales = scrapSales(doc);

         String currentSeller = sellerFullName;
         if (isMainRetailer && !mainSellerNameLower.isEmpty()) currentSeller = mainSellerNameLower;

         offers.add(OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(currentSeller)
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

   private boolean isMainRetailer(String sellerFullName) {
      boolean isMainRetailer = false;

      for (String sellerName : sellersVariations) {
         sellerName = StringUtils.stripAccents(sellerName.toLowerCase(Locale.ROOT));
         if (sellerName.equalsIgnoreCase(sellerFullName)) {
            isMainRetailer = true;
         }
      }
      return isMainRetailer;
   }

   private boolean checkIsMainRetailer(String sellerFullName) {
      boolean isMainRetailer = false;

      if (sellerFullName != null) {
         sellerFullName = StringUtils.stripAccents(sellerFullName.toLowerCase(Locale.ROOT));

         if (sellersVariations != null && !sellersVariations.isEmpty()) {
            isMainRetailer = isMainRetailer(sellerFullName);
         } else {
            String mainSellerNameLowerWithoutAccents = StringUtils.stripAccents(mainSellerNameLower.toLowerCase(Locale.ROOT));
            if (mainSellerNameLowerWithoutAccents.equalsIgnoreCase(sellerFullName) || sellerFullName.contains(mainSellerNameLowerWithoutAccents)) {
               isMainRetailer = true;
            }
         }

      }

      return isMainRetailer;
   }

   private void scrapSellersPage(Offers offers, Document doc, boolean hasMainOffer) throws OfferException, MalformedPricingException {
      String sellersPageUrl = CrawlerUtils.scrapUrl(doc, ".ui-pdp-other-sellers__link", "href", "https", "www.mercadolivre.com.br");
      if (sellersPageUrl == null) {
         sellersPageUrl = CrawlerUtils.scrapUrl(doc, ".ui-pdp-products__list a", "href", "https", "www.mercadolivre.com.br");
      }
      if (sellersPageUrl == null) {
         sellersPageUrl = CrawlerUtils.completeUrl(CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".ui-pdp-actions__container.ui-pdp-actions__container--one-element button", "formaction"), "https", "www.mercadolivre.com.br");
      }

      if (sellersPageUrl != null) {
         String nextUrl = sellersPageUrl;

         int sellersPagePosition = 1;
         boolean mainOfferFound = false;
         String spotlightSellerName = hasMainOffer ? offers.getOffersList().get(0).getSellerFullName() : null;

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
                  if (hasMainOffer && sellerName != null && !mainOfferFound && spotlightSellerName.toLowerCase(Locale.ROOT).contains(sellerName.toLowerCase(Locale.ROOT))) {
                     Offer offerMainPage = offers.getSellerByName(spotlightSellerName);
                     offerMainPage.setMainPagePosition(sellersPagePosition);
                     mainOfferFound = true;


                  } else {
                     boolean sellerNameIsMainRetailer = checkIsMainRetailer(sellerName);
                     String currentSeller = sellerName;
                     if (sellerNameIsMainRetailer && !mainSellerNameLower.isEmpty()) currentSeller = mainSellerNameLower;
                     if ((sellerNameIsMainRetailer || allow3PSellers) && currentSeller != null) {
                        Pricing pricing = scrapPricing(e);
                        List<String> sales = scrapSales(e);
                        offers.add(OfferBuilder.create()
                           .setUseSlugNameAsInternalSellerId(true)
                           .setSellerFullName(currentSeller)
                           .setSellersPagePosition(sellersPagePosition)
                           .setIsBuybox(true)
                           .setIsMainRetailer(sellerNameIsMainRetailer)
                           .setPricing(pricing)
                           .setSales(sales)
                           .build());

                     }
                  }

                  sellersPagePosition++;

               }
            } else {
               break;
            }

         } while (nextUrl != null);
      } else {
         if (offers.isEmpty() && allow3PSellers) {

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

   private Pricing scrapPricing(Element doc, Double spotlightPrice) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "del.price-tag", null, false, ',', session);
      if (priceFrom == null) {
         priceFrom = scrapPricingFromSellersPage(doc);
         if (priceFrom == spotlightPrice) {
            priceFrom = null;
         }
      }

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

   private Pricing scrapPricing(Element doc) throws MalformedPricingException {
      Double spotlightPrice = findSpotlightPrice(doc);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "del.price-tag", null, false, ',', session);
      if (priceFrom == null) {
         priceFrom = scrapPricingFromSellersPage(doc);
         if (priceFrom == spotlightPrice) {
            priceFrom = null;
         }
      }

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
         price = CrawlerUtils.scrapDoublePriceFromHtml(doc, "span.andes-money-amount.ui-pdp-price__part.andes-money-amount--cents-superscript meta", "content", false, '.', session);
      }
      if (price == null) {
         price = CrawlerUtils.scrapDoublePriceFromHtml(doc, "div.ui-pdp-price span.price-tag-amount", null, false, ',', session);
      }
      if (price == null) { // for when called to scrap price on a sellers page
         price = scrapSpotlightPricingFromSellersPage(doc);
      }

      return price;
   }

   private Double scrapSpotlightPricingFromSellersPage(Element doc) {
      Integer priceFraction = CrawlerUtils.scrapIntegerFromHtml(doc, ".ui-pdp-price__second-line .andes-money-amount__fraction", false, 0);
      Integer priceCents = CrawlerUtils.scrapIntegerFromHtml(doc, ".ui-pdp-price__second-line .andes-money-amount__cents", false, 0);

      if (priceFraction == 0) {
         priceFraction = CrawlerUtils.scrapIntegerFromHtml(doc, ".ui-pdp-price .andes-money-amount__fraction", false, 0);
         priceCents = CrawlerUtils.scrapIntegerFromHtml(doc, ".ui-pdp-price .andes-money-amount__cents", false, 0);
      }

      return priceFraction + (double) priceCents / 100;
   }

   private Double scrapPricingFromSellersPage(Element doc) {

      Integer priceFraction = CrawlerUtils.scrapIntegerFromHtml(doc, ".ui-pdp-price__original-value .andes-money-amount__fraction", false, 0);
      Integer priceCents = CrawlerUtils.scrapIntegerFromHtml(doc, ".ui-pdp-price__original-value .andes-money-amount__cents", false, 0);

      if (priceFraction == 0 && priceCents == 0) {
         return null;
      }

      return priceFraction + (double) priceCents / 100;
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

   public JSONObject selectJsonFromHtml(Document doc) {

      if (doc == null)
         throw new IllegalArgumentException("Argument doc cannot be null");
      String token = "window.__PRELOADED_STATE__";
      JSONObject object = null;
      Elements scripts = doc.select("script");

      for (Element e : scripts) {
         String script = e.html();

         if (script.contains(token)) {
            String stringToConvertInJson;
            if (script.contains("shopModel")) {
               stringToConvertInJson = getObject(script);
               if (!stringToConvertInJson.isEmpty()) {
                  object = CrawlerUtils.stringToJson(stringToConvertInJson);
               }
            } else {
               stringToConvertInJson = getObjectSecondOption(script);
               object = CrawlerUtils.stringToJson(stringToConvertInJson);
            }
            break;
         }
      }

      return object;
   }

   private String getObjectSecondOption(String script) {
      String json = null;
      Pattern pattern = Pattern.compile("initialState\":(.*)?,\"csrfToken\"");
      Matcher matcher = pattern.matcher(script);
      if (matcher.find()) {
         json = matcher.group(1);
      }
      return json;
   }

   private String getObject(String script) {
      String json = null;
      Pattern pattern = Pattern.compile("initialState\":(.*)?,\"site\"");
      Matcher matcher = pattern.matcher(script);
      if (matcher.find()) {
         json = matcher.group(1);
      }
      return json;
   }
}
