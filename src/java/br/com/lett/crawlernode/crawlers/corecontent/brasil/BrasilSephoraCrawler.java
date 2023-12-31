package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
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

import java.util.*;

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
      super.config.setParser(Parser.HTML);
   }

   @Override
   protected Response fetchResponse() {
      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "*/*");
      headers.put("Accept-Encoding", "gzip, deflate, br");
      headers.put("Connection", "keep-alive");
      headers.put("x-requested-with", "XMLHttpRequest");

      Request request = Request.RequestBuilder.create()
         .setUrl(session.getOriginalURL())
         .setHeaders(headers)
         .setProxyservice(List.of(ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_MX))
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher(), new FetcherDataFetcher()), session, "get");

      return response;
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
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-name-small .product-name", true);

         Elements variants = doc.select(".variation-content li");
         for (Element variant : variants) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(variant, "meta[itemprop=sku]", "content");
            String variantUrl = "https://www.sephora.com.br/on/demandware.store/Sites-Sephora_BR-Site/pt_BR/Product-Variation?pid=" + internalId;
            String variantName = scrapName(variant, name);

            Document variantProductPage = fetchVariantProductPage(variantUrl);

            List<String> secondaryImages = crawlImages(variantProductPage);
            String primaryImage = secondaryImages.isEmpty() ? null : secondaryImages.remove(0);

            boolean isAvailable = variant.select(".not-selectable").isEmpty();
            Offers offers = isAvailable ? scrapOffers(variantProductPage) : new Offers();
            RatingsReviews ratingsReviews = crawlRatingReviews(internalPid);

            Product product = ProductBuilder.create()
               .setUrl(variantUrl)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(variantName)
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

   private Document fetchVariantProductPage(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "*/*");
      headers.put("Accept-Encoding", "gzip, deflate, br");
      headers.put("Connection", "keep-alive");
      headers.put("x-requested-with", "XMLHttpRequest");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setProxyservice(List.of(ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_MX))
         .build();

      String response = CrawlerUtils.retryRequestString(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher(), new FetcherDataFetcher()), session);

      return Jsoup.parse(response);
   }

   private String scrapName(Element variant, String name) {
      String variantName = CrawlerUtils.scrapStringSimpleInfo(variant, ".variation-text-name", true);

      if (variantName != null) {
         return name + " - " + variantName;
      }

      return name;
   }

   private List<String> crawlImages(Document productPage) {
      List<String> secondaryImagesArray = new ArrayList<>();

      Elements imagesList = productPage.select(".show-for-medium .product-thumbnails .thumb > a");

      if (imagesList.isEmpty()) {
         imagesList = productPage.select(".product-thumbnails .thumb > a");
      }
      if (!imagesList.isEmpty()) {
         for (Element imageElement : imagesList) {
            String imgURL = CrawlerUtils.scrapStringSimpleInfoByAttribute(imageElement, "a", "href");
            if (imgURL != null) {
               secondaryImagesArray.add(imgURL);
            }
         }
      }

      return secondaryImagesArray;
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
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price-sales span", null, false, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price-box .price-standard", null, false, ',', session);

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
