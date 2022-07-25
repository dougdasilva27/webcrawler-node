package br.com.lett.crawlernode.crawlers.extractionutils.core;

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
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Data: 05/02/2021
 *
 * @author BuSSoLoTTi
 */

public class FalabellaCrawler extends Crawler {

   public FalabellaCrawler(Session session) {
      super(session);
   }
   protected Set<Card> cards = Sets.newHashSet(Card.VISA, Card.MASTERCARD, Card.AMEX);

   private final boolean allow3pSeller = isAllow3pSeller();
   protected boolean isAllow3pSeller() {
      return session.getOptions().optBoolean("allow_3p_seller", true);
   }

   private final String HOME_PAGE = getHomePage();
   protected String getHomePage() {
      return session.getOptions().optString("home_page");
   }

   private final String API_CODE = getApiCode();
   protected String getApiCode() {
      return session.getOptions().optString("api_code");
   }

   private final String SELLER_FULL_NAME = getSellerName();
   protected String getSellerName() {
      return session.getOptions().optString("seller_name");
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         String sellerFullName = scrapSellerFullName(doc);
         boolean isMainSeller = sellerFullName.equalsIgnoreCase(SELLER_FULL_NAME);

         if (isMainSeller || allow3pSeller) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "div[data-id]", "data-id");
            if(internalId == null){
               internalId = getReviewId(session.getOriginalURL());
            }
            String internalPid = internalId;
            String name = crawlBrandName(doc);
            boolean available = doc.select(".availability span").size() > 1;
            Offers offers = available ? scrapOffers(doc, sellerFullName, isMainSeller) : null;
            CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb");

            List<String> images = scrapImages(doc);
            String primaryImage = images != null ? images.remove(0) : null;

            String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#productInfoContainer"));

            RatingsReviews ratingsReviews = scrapRatingsReviews(internalId, session.getOriginalURL());

            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setOffers(offers)
               .setCategories(categories)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(images)
               .setDescription(description)
               .setRatingReviews(ratingsReviews)
               .build();

            products.add(product);
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private String crawlBrandName(Document doc) {
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, "div[data-name]", true);
      String brand = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "div[data-brand]", "data-brand");
      if (brand != null && !brand.isEmpty()) {
         name = name + " - " + brand;
         return name;
      }
      return name;
   }

   private RatingsReviews scrapRatingsReviews(String internalId, String url) {
      RatingsReviews ratingsReviews = new RatingsReviews();

      ratingsReviews.setDate(session.getDate());
      ratingsReviews.setInternalId(internalId);

      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingsReviews(url);

      ratingsReviews.setAdvancedRatingReview(advancedRatingReview);
      ratingsReviews.setAverageOverallRating(CrawlerUtils.extractRatingAverageFromAdvancedRatingReview(advancedRatingReview));
      ratingsReviews.setTotalRating(CrawlerUtils.extractReviwsNumberOfAdvancedRatingReview(advancedRatingReview));

      return ratingsReviews;
   }

   private AdvancedRatingReview scrapAdvancedRatingsReviews(String url) {
      AdvancedRatingReview advancedRatingReview = new AdvancedRatingReview();
      String idProductReview = getReviewId(url);
      String urlReview = "https://api.bazaarvoice.com/data/display/0.2alpha/product/summary?PassKey=m8bzx1s49996pkz12xvk6gh2e&productid=" + idProductReview + "&contentType=reviews&reviewDistribution=primaryRating&rev=0";

      Request request = Request.RequestBuilder.create()
         .setUrl(urlReview)
         .build();

      String response = dataFetcher.get(session, request).getBody();

      JSONObject json = response != null ? new JSONObject(response) : new JSONObject();

      advancedRatingReview.setTotalStar1(JSONUtils.getValueRecursive(json, "reviewSummary.primaryRating.distribution.4.count", Integer.class));
      advancedRatingReview.setTotalStar2(JSONUtils.getValueRecursive(json, "reviewSummary.primaryRating.distribution.3.count", Integer.class));
      advancedRatingReview.setTotalStar3(JSONUtils.getValueRecursive(json, "reviewSummary.primaryRating.distribution.2.count", Integer.class));
      advancedRatingReview.setTotalStar4(JSONUtils.getValueRecursive(json, "reviewSummary.primaryRating.distribution.1.count", Integer.class));
      advancedRatingReview.setTotalStar5(JSONUtils.getValueRecursive(json, "reviewSummary.primaryRating.distribution.0.count", Integer.class));

      return advancedRatingReview;
   }

   private String getReviewId(String url) {
      String regex = "product/([0-9]*)/";

      Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      Matcher matcher = pattern.matcher(url);
      if (matcher.find()) {
         return matcher.group(1);
      }
      return null;
   }

   private List<String> scrapImages(Document doc) {
      Element imageScript = doc.selectFirst("script#__NEXT_DATA__");
      if (imageScript != null) {
         JSONObject imageToJson = CrawlerUtils.stringToJson(imageScript.html());
         JSONArray imageArray = JSONUtils.getValueRecursive(imageToJson, "props.pageProps.productData.variants.0.medias", JSONArray.class);
         if (imageArray.length() == 0){
            imageArray = JSONUtils.getValueRecursive(imageToJson, "props.pageProps.productData.medias", JSONArray.class);
         }
         List<String> images = new ArrayList<>();
         for (int i = 0; i < imageArray.length(); i++) {
            String imageList = JSONUtils.getValueRecursive(imageArray, i + ".url", String.class);
            images.add(imageList);
         }
         return images;
      }
      return null;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".productContainer") != null;
   }

   private Offers scrapOffers(Document doc, String sellerFullName, Boolean isMainSeller ) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();

      Pricing pricing = scrapPricing(doc);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(sellerFullName)
         .setPricing(pricing)
         .setIsBuybox(false)
         .setIsMainRetailer(isMainSeller)
         .build());

      return offers;
   }

   private String scrapSellerFullName(Document doc) {
      return CrawlerUtils.scrapStringSimpleInfo(doc, ".sellerInfoContainer .underline", true);
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {

      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "li[data-internet-price]", "data-internet-price", true, ',', session);
      if(spotlightPrice == null){
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "li[data-event-price]", "data-event-price", true, ',', session);
      }
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "li[data-normal-price]", "data-normal-price", true, ',', session);
      Double alternativePrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "li[data-cmr-price]", "data-cmr-price", true, ',', session);

      if (alternativePrice != null) {
         priceFrom = spotlightPrice;
         spotlightPrice = alternativePrice;
      }

      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      for (Card card : cards) {

         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setIsShopCard(false)
            .setBrand(card.toString())
            .setInstallments(installments)
            .build());
      }

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
   }
}
