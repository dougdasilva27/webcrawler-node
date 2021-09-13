package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
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
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * date: 05/09/2018
 *
 * @author gabriel
 * @author gabriel
 */

//this website does not have more ratings.

public class BrasilSephoraCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.sephora.com.br/";
   private static final String MAIN_SELLER_NAME_LOWER = "sephora";


   public BrasilSephoraCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-number.show-for-medium>span", "data-masterid");
         String description = CrawlerUtils.scrapStringSimpleInfo(doc, ".tabs-panel.is-active", false);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "div.breadcrumb-element", true);

         Elements variants = doc.select(".product-detail .product-variations .no-bullet li div[itemprop]");
         for (Element variant : variants) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(variant, "meta[itemprop=sku]", "content");
            Document variantProductPage = fetchVariantProductPage(internalId);
            String name = scrapName(variantProductPage);
            List<String> secondaryImages = crawlImages(variantProductPage);
            String primaryImage = secondaryImages.isEmpty() ? null : secondaryImages.remove(0);

            boolean isAvailable = variantProductPage.select(".not-available-msg").isEmpty();
            Offers offers = isAvailable ? scrapOffers(variantProductPage) : new Offers();
            RatingsReviews ratingsReviews = crawlRatingReviews(internalPid);

            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setCategories(categories)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setDescription(description)
               .setOffers(offers)
               .setRatingReviews(ratingsReviews)
               .build();

            products.add(product);
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document document) {
      return document.selectFirst(".product-cart") != null;
   }

   private String scrapName(Document doc) {
      return CrawlerUtils.scrapStringSimpleInfo(doc, ".product-name-small-wrapper", false)
         + " - "
         + CrawlerUtils.scrapStringSimpleInfo(doc, "span.selected-value-name", false);
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();

      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(pricing);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(MAIN_SELLER_NAME_LOWER)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());
      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price-sales.price-sales-standard span:first-child", null, false, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "span.price-standard", null, false, ',', session);

      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);

      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();
   }

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();

      String saleDiscount = CrawlerUtils.calculateSales(pricing);
      if (saleDiscount != null) {
         sales.add(saleDiscount);
      }

      return sales;
   }

   private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Set<Card> cards = Sets.newHashSet(
         Card.VISA,
         Card.MASTERCARD,
         Card.AURA,
         Card.DINERS,
         Card.HIPER,
         Card.AMEX
      );

      Installments installments = scrapInstallments(doc);

      if (installments.getInstallments().isEmpty()) {

         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());

      }
      for (Card card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card.toString())
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   private Installments scrapInstallments(Document doc) throws MalformedPricingException {
      Installments installments = new Installments();

      String[] pairInstallment = null;
      Element installmentElem = doc.selectFirst(".installments.installments-pdp");
      if (installmentElem != null) {
         String text = installmentElem.text();
         if (text != null) {
            pairInstallment = text.split("\\sde\\s");
         }
      }

      if (Objects.nonNull(pairInstallment) && pairInstallment.length >= 2) {
         installments.add(
            Installment.InstallmentBuilder.create()
               .setInstallmentNumber(MathUtils.parseInt(pairInstallment[0]))
               .setInstallmentPrice(MathUtils.parseDoubleWithDot(pairInstallment[1]))
               .build()
         );
      }

      return installments;
   }

   private List<String> crawlImages(Document productPage) {
      List<String> secondaryImagesArray = new ArrayList<>();

      Elements imagesList = productPage.select("div.show-for-small-only.text-center ul li.thumb>a");

      if (!imagesList.isEmpty()) {
         for (Element imageElement : imagesList) {
            secondaryImagesArray.add(CrawlerUtils.scrapStringSimpleInfoByAttribute(imageElement, "a", "href"));
         }
      }

      return secondaryImagesArray;
   }

   private Document fetchVariantProductPage(String internalPid) {
      String url = "https://www.sephora.com.br/on/demandware.store/Sites-Sephora_BR-Site/pt_BR/Product-Variation?pid=" + internalPid + "&format=ajax";

      Request request = Request.RequestBuilder.create().setUrl(url).build();
      String response = this.dataFetcher.get(session, request).getBody();

      return Jsoup.parse(response);
   }

   private RatingsReviews crawlRatingReviews(String partnerId) {
      RatingsReviews ratingReviews = new RatingsReviews();

      ratingReviews.setDate(session.getDate());

      final String bazaarVoicePassKey = "caE4uptBcLh9IQxL7yvSpi2JFuIYLMs7ruBRlxIezCrzM";
      String endpointRequest = assembleBazaarVoiceEndpointRequest(partnerId, bazaarVoicePassKey, 0, 50);

      Request request = Request.RequestBuilder.create().setUrl(endpointRequest).setCookies(cookies).build();
      JSONObject ratingReviewsEndpointResponse = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
      JSONObject reviewStatistics = getReviewStatisticsJSON(ratingReviewsEndpointResponse, partnerId);
      AdvancedRatingReview advRating = scrapAdvancedRatingReview(reviewStatistics);
      Integer total = getTotalReviewCount(ratingReviewsEndpointResponse);

      ratingReviews.setTotalRating(total);
      ratingReviews.setTotalWrittenReviews(total);
      ratingReviews.setAverageOverallRating(getAverageOverallRating(reviewStatistics));
      ratingReviews.setAdvancedRatingReview(advRating);

      return ratingReviews;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(JSONObject JsonRating) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;


      if (JsonRating.has("RatingDistribution")) {
         JSONArray ratingDistribution = JsonRating.optJSONArray("RatingDistribution");

         for (int i = 0; i < ratingDistribution.length(); i++) {
            JSONObject rV = ratingDistribution.optJSONObject(i);


            int val1 = rV.optInt("RatingValue");
            int val2 = rV.optInt("Count");

            switch (val1) {
               case 5:
                  star5 = val2;
                  break;
               case 4:
                  star4 = val2;
                  break;
               case 3:
                  star3 = val2;
                  break;
               case 2:
                  star2 = val2;
                  break;
               case 1:
                  star1 = val2;
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

   private Integer getTotalReviewCount(JSONObject reviewStatistics) {
      Integer totalReviewCount = 0;

      JSONArray results = JSONUtils.getJSONArrayValue(reviewStatistics, "Results");
      for (Object result : results) {
         JSONObject resultObject = (JSONObject) result;

         String locale = JSONUtils.getStringValue(resultObject, "ContentLocale");
         if (locale != null && locale.equalsIgnoreCase("pt_BR")) { // this happen because fastshop only show reviews from brasil
            totalReviewCount++;
         }
      }

      return totalReviewCount;
   }

   private Double getAverageOverallRating(JSONObject reviewStatistics) {
      Double avgOverallRating = 0d;
      if (reviewStatistics.has("AverageOverallRating")) {
         avgOverallRating = reviewStatistics.optDouble("AverageOverallRating");
      }
      return avgOverallRating;
   }

   private String assembleBazaarVoiceEndpointRequest(String skuInternalPid, String bazaarVoiceEnpointPassKey, Integer offset, Integer limit) {

      StringBuilder request = new StringBuilder();

      request.append("http://api.bazaarvoice.com/data/reviews.json?apiversion=5.5");
      request.append("&passkey=" + bazaarVoiceEnpointPassKey);
      request.append("&Offset=" + offset);
      request.append("&Limit=" + limit);
      request.append("&Sort=SubmissionTime:desc");
      request.append("&Filter=ProductId:" + skuInternalPid);
      request.append("&Include=Products");
      request.append("&Stats=Reviews");

      return request.toString();
   }


   private JSONObject getReviewStatisticsJSON(JSONObject ratingReviewsEndpointResponse, String skuInternalPid) {
      if (ratingReviewsEndpointResponse.has("Includes")) {
         JSONObject includes = ratingReviewsEndpointResponse.optJSONObject("Includes");

         if (includes.has("Products")) {
            JSONObject products = includes.optJSONObject("Products");

            if (products.has(skuInternalPid)) {
               JSONObject product = products.optJSONObject(skuInternalPid);

               if (product.has("ReviewStatistics")) {
                  return product.optJSONObject("ReviewStatistics");
               }
            }
         }
      }

      return new JSONObject();
   }
}
