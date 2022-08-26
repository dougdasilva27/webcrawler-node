package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
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

import java.util.*;

//the first crawler made with github copilot
public class BrasilCobasiCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.cobasi.com.br/";
   private static final String SELLER_FULL_NAME = "cobasi";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());


   public BrasilCobasiCrawler(Session session) {
      super(session);
   }


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {

      List<Product> products = new ArrayList<>();
      JSONObject pageJson = CrawlerUtils.selectJsonFromHtml(doc, "#__NEXT_DATA__", null, null, false, false);


      if ((pageJson != null && !pageJson.isEmpty()) && pageJson.query("/props/pageProps/productDetail") != null) {


         JSONObject productsObj = (JSONObject) pageJson.query("/props/pageProps/productDetail");
         JSONArray variants = productsObj.optJSONArray("activeSkus");
         CategoryCollection categoryCollection = scrapeCategory(productsObj);
         List<String> images = scrapeImages(productsObj);
         String description = scrapDescription(productsObj);

         String primaryImage = images.isEmpty() ? null : images.remove(0);
         List<String> secondaryImages = images.isEmpty() ? null : images;

         for (Object o : variants) {

            JSONObject variant = (JSONObject) o;
            Offers offers = scrapOffer(variant);
            RatingsReviews ratingsReviews = crawlRating(productsObj);
            String name = crawlName(productsObj, variant, doc);
            String id = variant.optString("id");
            String pid = productsObj.optString("id");
            String[] urlarr = session.getOriginalURL().split("=");
            String url = urlarr[0].contains("?idsku") ? urlarr[0] + "=" + id : urlarr[0] + "?idsku=" + id;

            Product product = ProductBuilder.create()
               .setUrl(url)
               .setInternalId(id)
               .setInternalPid(pid)
               .setName(name)
               .setCategories(categoryCollection)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setDescription(description)
               .setOffers(offers)
               .setRatingReviews(ratingsReviews)
               .setEans(Collections.singletonList(variant.optString("ean")))
               .build();

            products.add(product);

         }
      } else {
         Logging.printLogDebug(logger, "No products page");
      }
      return products;
   }

   private String scrapDescription(JSONObject productsObj) {

      String description = productsObj.optString("description", "");
      String descriptionShort = productsObj.optString("descriptionShort", "");


      return description + "\n" + descriptionShort;

   }

   private String crawlName(JSONObject productsObj, JSONObject variant, Document doc) {
      String name = productsObj.optString("name");
      String brand = CrawlerUtils.scrapStringSimpleInfo(doc, ".styles__BrandLink-sc-1rue5eb-14", true);
      String nameVariant = variant.optString("name");

      if (nameVariant != null) {
         name = name + " " + nameVariant;
      }

      if (brand != null) {
         name = name + " - " + brand;
      }

      return name;


   }

   private Offers scrapOffer(JSONObject variation) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();

      if (variation.optBoolean("available")) {

         Pricing pricing = scrapPricing(variation);

         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(SELLER_FULL_NAME)
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .build());

      }
      return offers;

   }


   private Pricing scrapPricing(JSONObject jsonObject) throws MalformedPricingException {
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(jsonObject, "bestPriceFormated", false);
      Double priceFrom = JSONUtils.getDoubleValueFromJSON(jsonObject, "listPriceFormated", false);
      if (priceFrom == 0) {
         priceFrom = null;
      }

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
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

   private List<String> scrapeImages(JSONObject productsObj) {
      List<String> images = new ArrayList<>();
      JSONArray imagesArray = productsObj.optJSONArray("imagesAndVideos");
      if (imagesArray != null) {
         for (int i = 0; i < imagesArray.length(); i++) {
            images.add(imagesArray.optJSONObject(i).optString("imageUrl"));
         }
      }
      return images;
   }

   private CategoryCollection scrapeCategory(JSONObject jsonObject) {
      CategoryCollection categoryCollection = new CategoryCollection();
      String categories = (String) jsonObject.query("/categories/0");
      categoryCollection.addAll(Arrays.asList(categories));
      return categoryCollection;
   }

   private RatingsReviews crawlRating(JSONObject jsonObject) {
      JSONObject aggregationRating = (JSONObject) jsonObject.optQuery("/productRating");
      RatingsReviews ratingsReviews = new RatingsReviews();

      if (aggregationRating != null) {
         AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(aggregationRating);

         ratingsReviews.setTotalRating(aggregationRating.optInt("total_opinions"));
         ratingsReviews.setAdvancedRatingReview(advancedRatingReview);
         ratingsReviews.setAverageOverallRating(aggregationRating.optDouble("avg", 0d));
      }
      return ratingsReviews;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(JSONObject reviews) {

      JSONObject reviewValue = reviews.optJSONObject("stars");

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
