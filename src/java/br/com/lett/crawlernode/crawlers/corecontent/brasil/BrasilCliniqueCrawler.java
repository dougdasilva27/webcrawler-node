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
import org.jsoup.select.Elements;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilCliniqueCrawler extends Crawler {


   private static final String SELLER_FULL_NAME = "clinique";
   private static String MERCHANT_ID = "";
   private static String RATING_APIKEY = "";

   public BrasilCliniqueCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "meta[name=productId]", "content");
         String description = CrawlerUtils.scrapStringSimpleInfo(doc, "div.abstract", false);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "ul.breadcrumbs > li");
         RatingsReviews ratingReviews = crawlRatingReviews(doc, internalPid);

         Elements variants = doc.select("div[itemprop=offers]");

         if (variants != null && !variants.isEmpty()) {
            for (Element variant : variants) {

               String name = CrawlerUtils.scrapStringSimpleInfoByAttribute(variant, "meta[itemprop=name]", "content");
               String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(variant, "meta[itemprop=sku]", "content");
               String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "img[data-skuid=" + internalId + "]",
                  Collections.singletonList("data-src"),
                  "https",
                  "www.clinique.com.br");
               boolean isAvailable = CrawlerUtils.scrapStringSimpleInfoByAttribute(variant, "meta[itemprop=availability]", "content").equals("In Stock");

               JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "[id=page_data]", null, null, false, false);
               Offers offers = isAvailable && json != null ? scrapOffers(json, internalId) : new Offers();

               // Creating the product
               Product product = ProductBuilder.create()
                  .setUrl(session.getOriginalURL())
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setCategories(categories)
                  .setPrimaryImage(primaryImage)
                  .setDescription(description)
                  .setOffers(offers)
                  .setRatingReviews(ratingReviews)
                  .build();

               products.add(product);
            }
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".page-product") != null;
   }

   private RatingsReviews crawlRatingReviews(Document doc, String internalId) {
      RatingsReviews ratingReviews = new RatingsReviews();
      String id = internalId.replace("PROD", "");

      JSONObject ratingsJson = fetchJsonRatings(doc, id);

      ratingReviews.setDate(session.getDate());
      int totalRating = JSONUtils.getValueRecursive(ratingsJson, "paging.total_results", Integer.class);
      ratingReviews.setTotalRating(totalRating);
      ratingReviews.setTotalWrittenReviews(totalRating);
      ratingReviews.setAdvancedRatingReview(scrapAdvancedRatingReview(ratingsJson));
      ratingReviews.setAverageOverallRating(scrapAverageRating(ratingsJson));

      return ratingReviews;
   }

   private JSONObject fetchJsonRatings(Document doc, String id) {
      scrapRatingKeys(doc);
      String url = "https://display.powerreviews.com/m/" + MERCHANT_ID + "/l/pt_BR/product/" + id + "/reviews?_noconfig=true&apikey=" + RATING_APIKEY;

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .build();

      return CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
   }

   private void scrapRatingKeys(Document doc) {
      Element script = doc.selectFirst("script[defer][onload]");

      if (script != null) {
         String scriptStr = script.attr("onload");

         Pattern pattern = Pattern.compile("\"apiKey\":\"(.*?)\".*\"merchantId\":\"(.*?)\"");
         Matcher matcher = pattern.matcher(scriptStr);

         if (matcher.find()) {
            RATING_APIKEY = matcher.group(1);
            MERCHANT_ID = matcher.group(2);
         }
      }
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(JSONObject jsonRating) {
      int star1 = 0;
      int star2 = 0;
      int star3 = 0;
      int star4 = 0;
      int star5 = 0;

      JSONArray resultsArray = jsonRating.optJSONArray("results");

      if (resultsArray != null && resultsArray.optJSONObject(0) != null) {

         JSONArray resultsJson = JSONUtils.getValueRecursive(resultsArray.optJSONObject(0), "rollup.rating_histogram", JSONArray.class);

         if (resultsJson != null) {
            star1 = resultsJson.optInt(0);
            star2 = resultsJson.optInt(1);
            star3 = resultsJson.optInt(2);
            star4 = resultsJson.optInt(3);
            star5 = resultsJson.optInt(4);
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

   private Double scrapAverageRating(JSONObject json) {
      Double averageRating = 0d;

      JSONArray resultsArray = json.optJSONArray("results");
      if (resultsArray != null && resultsArray.optJSONObject(0) != null) {
         averageRating = JSONUtils.getValueRecursive(resultsArray.optJSONObject(0), "rollup.average_rating", Double.class, 0d);
      }

      return averageRating;
   }

   private Offers scrapOffers(JSONObject json, String internalPid) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      JSONObject variantJson = new JSONObject();

      JSONArray jsonProducts = JSONUtils.getValueRecursive(json, "catalog-spp.products", JSONArray.class);

      if (jsonProducts != null && jsonProducts.length() > 0) {
         JSONObject products = (JSONObject) jsonProducts.opt(0);

         if (products != null) {
            JSONArray variants = products.optJSONArray("skus");

            for (Object e : variants) {
               JSONObject variant = (JSONObject) e;

               if (variant.optString("SKU_ID").equals(internalPid)) {
                  variantJson = variant;
                  break;
               }
            }
         }
      }

      Pricing pricing = scrapPricing(variantJson);
      List<String> sales = scrapSales(pricing);

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

   private Pricing scrapPricing(JSONObject json) throws MalformedPricingException {
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(json, "PRICE", false);
      Double priceFrom = JSONUtils.getDoubleValueFromJSON(json, "PRICE2", false);

      CreditCards creditCards = scrapCreditCards(json, spotlightPrice);

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

   private CreditCards scrapCreditCards(JSONObject json, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Set<Card> cards = Sets.newHashSet(
         Card.VISA,
         Card.MASTERCARD,
         Card.AURA,
         Card.DINERS,
         Card.HIPER,
         Card.AMEX
      );

      Installments installments = scrapInstallments(json, spotlightPrice);

      for (Card card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card.toString())
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   private Installments scrapInstallments(JSONObject json, Double spotlightPrice) throws MalformedPricingException {
      Installments installments = new Installments();

      String installmentsStr = json.optString("formattedInstallmentPrice");

      if (installmentsStr != null) {
         String installmentNumber = installmentsStr.substring(6, 7).replaceAll("[^0-9.]", "");
         String installmentPrice = installmentsStr.substring(14, 19).replaceAll("[^0-9.,]", "");

         installments.add(
            Installment.InstallmentBuilder.create()
               .setInstallmentNumber(Integer.parseInt(installmentNumber))
               .setInstallmentPrice(Double.parseDouble(installmentPrice.replace(",", ".")))
               .build()
         );
      } else {
         installments.add(
            Installment.InstallmentBuilder.create()
               .setInstallmentNumber(1)
               .setInstallmentPrice(spotlightPrice)
               .build());
      }

      return installments;
   }
}
