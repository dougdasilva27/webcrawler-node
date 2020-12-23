package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.google.common.collect.Sets;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.BankSlip;
import models.pricing.CreditCard;
import models.pricing.CreditCards;
import models.pricing.Installment;
import models.pricing.Installments;
import models.pricing.Pricing;

/**
 * date: 27/09/2018
 *
 * @author gabriel
 */

public class BrasilZattiniCrawler extends Crawler {


   private static final String PROTOCOL = "https:";
   private final String HOME_PAGE = "https://www.zattini.com.br/";
   private static final String SELLER_FULL_NAME = "zattini";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public BrasilZattiniCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      boolean availableToBuy = false;
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      JSONObject chaordicJson = crawlChaordicJson(doc);

      if (chaordicJson.length() > 0) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = crawlInternalPid(chaordicJson);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb li:not(:first-child) > a span", false);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#features"));

         // sku data in json
         JSONArray arraySkus = chaordicJson != null && chaordicJson.has("skus") ? chaordicJson.getJSONArray("skus") : new JSONArray();

         for (int i = 0; i < arraySkus.length(); i++) {
            JSONObject jsonSku = arraySkus.getJSONObject(i);

            String internalId = crawlInternalId(jsonSku);
            String name = crawlName(doc, jsonSku);
            Element notAvailable = doc.selectFirst(".showcase .text-not-avaliable");

            if (notAvailable != null) {
               availableToBuy =
                     jsonSku.has("status") && jsonSku.get("status").toString().equalsIgnoreCase("available") && !notAvailable.hasClass("text-not-avaliable");
            } else {
               availableToBuy = jsonSku.has("status") && jsonSku.get("status").toString().equalsIgnoreCase("available");
            }


            boolean available = availableToBuy;
            String primaryImage = crawlPrimaryImage(doc);
            String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".swiper-slide:not(.active) img",
                  Arrays.asList("data-src-large", "src"), PROTOCOL, "static.zattini.com.br", primaryImage);
            RatingsReviews ratingsReviews = scrapRatingsReviews(doc);
            Offers offers = available ? scrapOffer(chaordicJson) : new Offers();

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
                  .setRatingReviews(ratingsReviews)
                  .setOffers(offers)
                  .build();

            products.add(product);
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private String crawlPrimaryImage(Document doc) {
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".photo-figure > img", Arrays.asList("data-large-img-url", "src"), PROTOCOL,
            "static.zattini.com.br");
      if (primaryImage == null) {
         primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "div[class=\"text-not-avaliable\"] figure img", Arrays.asList("src"), PROTOCOL,
               "static.zattini.com.br");

      }

      return primaryImage;
   }

   /*******************
    * General methods *
    *******************/

   private String crawlInternalId(JSONObject json) {
      String internalId = null;

      if (json.has("sku")) {
         internalId = json.getString("sku").trim();
      }

      return internalId;
   }

   private String crawlInternalPid(JSONObject skuJson) {
      String internalPid = null;

      if (skuJson.has("id")) {
         internalPid = skuJson.get("id").toString();
      }

      return internalPid;
   }

   private String crawlName(Document doc, JSONObject skuJson) {
      StringBuilder name = new StringBuilder();

      Element nameElement = doc.selectFirst("h1[itemprop=name]");
      if (nameElement != null) {
         name.append(nameElement.ownText());

         if (skuJson.has("specs")) {
            JSONObject specs = skuJson.getJSONObject("specs");

            Set<String> keys = specs.keySet();

            for (String key : keys) {
               if (!key.equalsIgnoreCase("color")) {
                  name.append(" " + specs.get(key));
               }
            }
         }
      }

      return name.toString();
   }



   private RatingsReviews scrapRatingsReviews(Document doc) {
      RatingsReviews ratingsReviews = new RatingsReviews();
      ratingsReviews.setDate(session.getDate());

      Integer totalNumOfEvaluations = getTotalNumOfRatings(doc);

      ratingsReviews.setTotalRating(totalNumOfEvaluations);
      ratingsReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingsReviews.setAverageOverallRating(getTotalAvgRating(doc));

      return ratingsReviews;
   }

   /**
    * Number of ratings appear in html element
    *
    * @param doc
    * @return
    */
   private Integer getTotalNumOfRatings(Document doc) {
      Integer totalRating = 0;
      Element rating = doc.selectFirst(".reviews__customerFeedback  h3");

      if (rating != null) {
         String votes = rating.ownText().replaceAll("[^0-9]", "");

         if (!votes.isEmpty()) {
            totalRating = Integer.parseInt(votes);
         }
      }

      return totalRating;
   }


   private Double getTotalAvgRating(Document docRating) {
      Double avgRating = 0d;
      Element rating = docRating.selectFirst(".rating-box__value");

      if (rating != null) {
         String text = rating.text().replaceAll("[^0-9.]", "").trim();

         if (!text.isEmpty()) {
            avgRating = Double.parseDouble(text);
         }
      }

      return avgRating;
   }

   private JSONObject crawlChaordicJson(Document doc) {
      JSONObject skuJson = new JSONObject();

      Elements scripts = doc.select("script");

      for (Element e : scripts) {
         String script = e.outerHtml();


         if (script.contains("freedom.metadata.chaordic(")) {
            String token = "loader.js', '";
            int x = script.indexOf(token) + token.length();
            int y = script.indexOf("');", x);

            String json = script.substring(x, y);

            if (json.startsWith("{") && json.endsWith("}")) {
               try {
                  JSONObject chaordic = new JSONObject(json);

                  if (chaordic.has("product")) {
                     skuJson = chaordic.getJSONObject("product");
                  }
               } catch (Exception e1) {
                  Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e1));
               }
            }

            break;
         }
      }

      return skuJson;
   }



   private Offers scrapOffer(JSONObject chaordicJson) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(chaordicJson);
      List<String> sales = new ArrayList<>();

      offers.add(Offer.OfferBuilder.create()
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


   private Pricing scrapPricing(JSONObject chaordicJson) throws MalformedPricingException {
      Double priceFrom = chaordicJson.has("old_price") ? chaordicJson.getDouble("old_price") : null;
      Double spotlightPrice = chaordicJson.optDouble("price");
      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);

      return Pricing.PricingBuilder.create()
            .setPriceFrom(priceFrom)
            .setSpotlightPrice(spotlightPrice)
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

}
