
package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.*;
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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 15/04/2021
 *
 * @author Thainá Aguiar
 */


public class BrasilMaccosmeticosCrawler extends Crawler {
   public BrasilMaccosmeticosCrawler(Session session) {
      super(session);
   }


   private static final String SELLER_NAME = "Mac Cosmeticos";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());


   private Document fetchDocument() {

      String url = session.getOriginalURL().split("#!/shade/")[0];

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .build();
      String content = this.dataFetcher
         .get(session, request)
         .getBody();

      return Jsoup.parse(content);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (doc.selectFirst(".site-container .product-full") != null) {
         Logging.printLogDebug(
            logger, session, "Product page identified: " + this.session.getOriginalURL());

         String shade = getShade();

         Document docJson = fetchDocument();

         JSONObject productJson = docJson != null ? getData(docJson, shade) : null;

         if (productJson != null) {
            String internalId = productJson.optString("PRODUCT_CODE");
            String internalPid = productJson.optString("PRODUCT_ID");
            String name = getName(doc, productJson, shade);
            String description = productJson.optString("SHADE_DESCRIPTION");
            JSONArray imageJson = JSONUtils.getValueRecursive(productJson, "LARGE_IMAGE", JSONArray.class);
            List<String> images = imageJson != null ? CrawlerUtils.scrapImagesListFromJSONArray(imageJson, null, null, "https", "www.maccosmetics.com.br", session) : null;
            String primaryImage = images != null && !images.isEmpty() ? images.remove(0) : null;
            RatingsReviews ratingReviews = crawRating(internalPid);
            int inventStatus = productJson.optInt("INVENTORY_STATUS");
            boolean available = inventStatus == 1;
            Offers offers = available ? scrapOffers(productJson) : new Offers();


            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(images)
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


   private String getShade() throws UnsupportedEncodingException {
      String shadeEncoded = CommonMethods.getLast(session.getOriginalURL().split("/"));
      String shade = shadeEncoded != null ? URLDecoder.decode(shadeEncoded, "UTF-8") : null;

      return shade != null ? shade.replace("_", " ") : null;
   }

   private String getName(Document doc, JSONObject productJson, String shade) {
      StringBuilder stringBuilder = new StringBuilder();
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-full__name", true);
      String productSize = productJson.optString("PRODUCT_SIZE");

      if (name != null) {
         stringBuilder.append(name).append(" ");
         if (shade != null) {
            stringBuilder.append(shade).append(" ");
         }
         if (productSize != null) {
            stringBuilder.append(productSize);
         }
      }
      return stringBuilder.toString();

   }

   private JSONObject getData(Document docJson, String shade) {

      JSONObject jsonObject = CrawlerUtils.selectJsonFromHtml(docJson, "#page_data", null, null, false, false);
      JSONArray dataArray = JSONUtils.getValueRecursive(jsonObject, "catalog-spp.products.0.skus", JSONArray.class);

      //if it is more than one array, it means that the product has other "variations", but the url determines which one is by the parameter
      if (dataArray.length() > 1) {
         for (Object o : dataArray) {
            if (o instanceof JSONObject) {
               JSONObject product = (JSONObject) o;

               String shadeName = product.optString("SHADENAME");
               if (shadeName.equals(shade)) {
                  return product;
               }
            }

         }
      } else {
         return dataArray.optJSONObject(0);
      }
      return null;
   }

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();
      String sale = CrawlerUtils.calculateSales(pricing);
      if (sale != null) {
         sales.add(sale);
      }

      return sales;
   }


   private Offers scrapOffers(JSONObject productJson) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(productJson);
      List<String> sales = scrapSales(pricing);


      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());


      return offers;
   }

   private Pricing scrapPricing(JSONObject productJson) throws MalformedPricingException {
      Double priceFrom = JSONUtils.getDoubleValueFromJSON(productJson, "PRICE2", true);
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(productJson, "rs_sku_price", true);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

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


   private JSONObject getApiReviews(String internalPid) {

      String apiKey = "1d01c6a1-3fb0-429d-9b01-7b3d915933af";
      String storeId = "545682";
      String url = "https://display.powerreviews.com/m/" + storeId + "/l/pt_BR/product/" + internalPid.replace("PROD", "") + "/reviews?apikey=" + apiKey + "&_noconfig=true";

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .build();
      String content = this.dataFetcher
         .get(session, request)
         .getBody();

      return CrawlerUtils.stringToJson(content);
   }


   private RatingsReviews crawRating(String internalPid) {

      JSONObject productReviews = getApiReviews(internalPid);
      RatingsReviews ratingsReviews = new RatingsReviews();
      JSONObject results = productReviews != null ? JSONUtils.getValueRecursive(productReviews, "results.0.rollup", JSONObject.class) : null;

      if (results != null) {

         double avgReviews = JSONUtils.getDoubleValueFromJSON(results, "average_rating", true);
         int totalRating = JSONUtils.getIntegerValueFromJSON(results, "review_count", 0);
         AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(results);

         ratingsReviews.setAverageOverallRating(MathUtils.normalizeTwoDecimalPlaces(avgReviews));
         ratingsReviews.setTotalRating(totalRating);
         ratingsReviews.setTotalWrittenReviews(totalRating);
         ratingsReviews.setAdvancedRatingReview(advancedRatingReview);

         return ratingsReviews;
      }

      return ratingsReviews;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(JSONObject results) {
      JSONArray stars = results.optJSONArray("rating_histogram");

      if (stars != null) {
         Integer star1 = stars.getInt(0);
         Integer star2 = stars.getInt(1);
         Integer star3 = stars.getInt(2);
         Integer star4 = stars.getInt(3);
         Integer star5 = stars.getInt(4);

         return new AdvancedRatingReview.Builder()
            .totalStar1(star1)
            .totalStar2(star2)
            .totalStar3(star3)
            .totalStar4(star4)
            .totalStar5(star5)
            .build();
      } else {
         return new AdvancedRatingReview();
      }
   }


}
