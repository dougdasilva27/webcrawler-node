package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.TrustvoxRatingCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class BrasilNutrimaisvidaCrawler extends Crawler {

   private static final String SELLER_NAME_LOWER = "nutrimaisvida";
   private static final Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.DINERS.toString(), Card.AMEX.toString(), Card.ELO.toString());

   public BrasilNutrimaisvidaCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".yotpo.yotpo-main-widget ", "data-product-id");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".proBoxInfo.col-xs-12.col-sm-12.col-md-6.col-lg-6 > div.wrap-title > h1", false);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".col-md-6.col-lg-6 > div.description.velaGroup > p:nth-child(3)"));
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#ProductPhotoImg", Arrays.asList("src"), "https", "://nutrimaisvida.com.br/");
         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, ".img-responsive.hidden-xs",
            Arrays.asList("src"), "https", "://nutrimaisvida.com.br/", primaryImage);
         RatingsReviews ratingsReviews = crawlRating(internalId);
         boolean available = CrawlerUtils.scrapStringSimpleInfo(doc, ".proBoxInfo.col-xs-12.col-sm-12.col-md-6.col-lg-6 > div.wrap-title > span > span", false).contains("Disponível");
         Offers offers = available ? scrapOffers(doc) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setRatingReviews(ratingsReviews)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);


      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".proBoxPrimaryInner") != null;
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);

      if (pricing != null) {
         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(SELLER_NAME_LOWER)
            .setSellersPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .build());
      }

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {

      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".formAddToCart.clearfix > .formAddToCart__proPrice.clearfix > .priceProduct.priceCompare", null, false, ',', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#ProductPrice", null, false, ',', session);

      if (spotlightPrice == null) {
         spotlightPrice = priceFrom;
      }

      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
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
   private RatingsReviews crawlRating(String internalId) {
      String url = "https://api-cdn.yotpo.com/v1/widget/C3WJEQUAevWXtzD53PwS4IFnSgbOtw3MkvQXWmJj/products/" + internalId + "/reviews";
      RatingsReviews ratingsReviews = new RatingsReviews();

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setSendUserAgent(true)
         .build();
      Response response = new FetcherDataFetcher().get(session, request);
      JSONObject jsonObject = JSONUtils.stringToJson(response.getBody());
      JSONObject aggregationRating = (JSONObject) jsonObject.optQuery("/response/bottomline");

      if (aggregationRating != null) {
         AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(aggregationRating);

         ratingsReviews.setTotalRating(aggregationRating.optInt("total_review"));
         ratingsReviews.setAdvancedRatingReview(advancedRatingReview);
         ratingsReviews.setAverageOverallRating(aggregationRating.optDouble("average_score", 0d));
      }
      return ratingsReviews;
   }
   private AdvancedRatingReview scrapAdvancedRatingReview(JSONObject reviews) {

      JSONObject reviewValue = reviews.optJSONObject("star_distribution");

      if (reviewValue != null) {
         return new AdvancedRatingReview.Builder()
            .totalStar1(reviewValue.optInt("1"))
            .totalStar2(reviewValue.optInt("2"))
            .totalStar3(reviewValue.optInt("3"))
            .totalStar4(reviewValue.optInt("4"))
            .totalStar5(reviewValue.optInt("5"))
            .build();
      }

      return new AdvancedRatingReview();
   }
}
