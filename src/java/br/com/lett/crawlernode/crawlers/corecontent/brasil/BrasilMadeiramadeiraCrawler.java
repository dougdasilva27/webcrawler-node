package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
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
import org.jooq.tools.json.JSONParser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class BrasilMadeiramadeiraCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.madeiramadeira.com.br/";
   private static final String MAIN_SELLER = "MadeiraMadeira";

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.HIPERCARD.toString(), Card.ELO.toString(),
      Card.AMEX.toString(), Card.DINERS.toString());

   public BrasilMadeiramadeiraCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         String scriptFromHtml = CrawlerUtils.scrapScriptFromHtml(doc, "#__NEXT_DATA__");
         JSONArray jsonObject = new JSONArray(scriptFromHtml);
         JSONObject productInfo = JSONUtils.getValueRecursive(jsonObject, "0.props.pageProps.product", JSONObject.class);

         Integer internalPid = productInfo.getInt("id");
         String name = productInfo.getString("name") + " " + productInfo.getString("color");
         String description = productInfo.getString("description");
         Integer ean = productInfo.getInt("ean");
         List<String> eans = new ArrayList<>();
         eans.add(ean.toString());
         Boolean available = scrapAvailability(productInfo);
         Offers offers = available ? scrapOffers(productInfo) : new Offers();

         CategoryCollection categories = crawlCategories(productInfo);
         String categories1 = categories.size() > 0 ? categories.getCategory(0) : null;
         String categories2 = categories.size() > 1 ? categories.getCategory(1) : null;
         String categories3 = categories.size() > 2 ? categories.getCategory(2) : null;

         List<String> images = crawlImages(productInfo);
         String primaryImage = !images.isEmpty() ? images.remove(0) : null;
         List<String> secondaryImages = !images.isEmpty() ? images : null;
         RatingsReviews ratingsReviews = scrapRating(productInfo);

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalPid.toString())
            .setInternalPid(internalPid.toString())
            .setName(name)
            .setOffers(offers) //One offer for each installment
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setEans(eans)
            .setCategory1(categories1)
            .setCategory2(categories2)
            .setCategory3(categories3)
            .setRatingReviews(ratingsReviews)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private Offers scrapOffers(JSONObject product) throws MalformedPricingException, OfferException {

      Offers offers = new Offers();

      JSONObject offersJson = JSONUtils.getValueRecursive(product, "buyBox.0", JSONObject.class);
      String sellerName = JSONUtils.getValueRecursive(offersJson, "seller.name", String.class);
      Integer sellerId = JSONUtils.getValueRecursive(offersJson, "seller.id", Integer.class);
      JSONArray installmentsInfo = offersJson.optJSONArray("installmentToDisplay");

      for (Object installment : installmentsInfo) {
         Pricing pricing = scrapPricing(offersJson, installment);
         List<String> sales = new ArrayList<>();

         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(false)
            .setSellerFullName(MAIN_SELLER)
            .setInternalSellerId(sellerId.toString())
            .setSellerFullName(sellerName)
            .setMainPagePosition(1)
            .setIsBuybox(true)
            .setIsMainRetailer(sellerName != null && sellerName.equalsIgnoreCase(MAIN_SELLER))
            .setSales(sales)
            .setPricing(pricing)
            .build());
      }

      return offers;
   }

   private Pricing scrapPricing(JSONObject offer, Object installment) throws MalformedPricingException {

      JSONObject priceJson = offer.optJSONObject("price");
      Double priceFrom = priceJson.optDouble("fake");

      Double spotlightPrice = priceJson.optDouble("inCash");
      BankSlip bankSlipPrice = BankSlip.BankSlipBuilder.create().setFinalPrice(spotlightPrice).build();

      JSONObject maxInstallments = (JSONObject) installment;

      CreditCards creditCards = scrapCreditCards(maxInstallments, spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setBankSlip(bankSlipPrice)
         .setCreditCards(creditCards)
         .build();

   }

   private CreditCards scrapCreditCards(JSONObject installmentsInfo, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = scrapInstallments(installmentsInfo);

      if (installments.getInstallments().isEmpty()) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .setFinalPrice(spotlightPrice)
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

   private Installments scrapInstallments(JSONObject installmentsInfo) throws MalformedPricingException {
      Installments installments = new Installments();

      if (installmentsInfo != null){
         Double value = installmentsInfo.optDouble("value");
         Integer installmentsNumbers = installmentsInfo.optInt("number");
         Double finalPrice = installmentsInfo.optDouble("total");

         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(installmentsNumbers)
            .setInstallmentPrice(value)
            .setFinalPrice(finalPrice)
            .build());
      }
      return installments;
   }

   private boolean isProductPage(Document doc) {
      return !doc.select("body.black-friday-theme").isEmpty();
   }

   private RatingsReviews scrapRating(JSONObject product) {
      RatingsReviews ratingReviews = new RatingsReviews();

      Integer totalNumOfEvaluations = JSONUtils.getValueRecursive(product, "rating.rate.count", Integer.class);
      Double avgRating = JSONUtils.getValueRecursive(product, "rating.rate.average", Double.class);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setDate(session.getDate());

      return ratingReviews;
   }

   private Boolean scrapAvailability(JSONObject product) {
      Integer stock = JSONUtils.getValueRecursive(product, "buyBox.0.stock", Integer.class);

      if (stock > 0) {
         return true;
      }
      return false;
   }

   public static CategoryCollection crawlCategories(JSONObject product) {
      CategoryCollection categories = new CategoryCollection();
      JSONArray jsonCategories = product.getJSONArray("breadcrumb");

      for (int i = 0; i < jsonCategories.length(); i++) {
         JSONObject categorieName = (JSONObject) jsonCategories.get(i);
         categories.add(categorieName.getString("name").trim());
      }

      return categories;
   }

   public static List<String> crawlImages(JSONObject product) {
      List<String> images = new ArrayList<>();
      JSONArray jsonCategories = product.getJSONArray("images");

      for (int i = 0; i < jsonCategories.length(); i++) {
         JSONObject imagesUrl = (JSONObject) jsonCategories.get(i);
         images.add(imagesUrl.getString("lst").trim());
      }

      return images;
   }
}
