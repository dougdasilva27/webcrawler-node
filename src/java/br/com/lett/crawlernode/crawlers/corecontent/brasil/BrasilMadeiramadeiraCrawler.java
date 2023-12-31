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

         int internalPid = productInfo.optInt("id");
         String name = productInfo.optString("name") + " " + productInfo.getString("color");
         String description = productInfo.optString("description");
         int ean = productInfo.optInt("ean");
         List<String> eans = new ArrayList<>();
         eans.add(Integer.toString(ean));
         boolean available = scrapAvailability(productInfo);
         Offers offers = available ? scrapOffers(productInfo) : new Offers();
         CategoryCollection categories = crawlCategories(productInfo);

         List<String> images = crawlImages(productInfo);
         String primaryImage = !images.isEmpty() ? images.remove(0) : null;
         List<String> secondaryImages = !images.isEmpty() ? images : null;
         RatingsReviews ratingsReviews = scrapRating(productInfo);

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(Integer.toString(internalPid))
            .setInternalPid(Integer.toString(internalPid))
            .setName(name)
            .setOffers(offers)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setEans(eans)
            .setCategories(categories)
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

      JSONArray offersJson = product.optJSONArray("buyBox");

      if (offersJson != null) {
         for (Object offerObject : offersJson) {
            if (!(offerObject instanceof JSONObject)) continue;

            JSONObject offer = (JSONObject) offerObject;
            String sellerName = JSONUtils.getValueRecursive(offer, "seller.name", String.class);
            Integer sellerId = JSONUtils.getValueRecursive(offer, "seller.id", Integer.class);
            JSONArray installmentsInfo = offer.optJSONArray("installmentToDisplay");
            Pricing pricing = scrapPricing(offer, installmentsInfo);
            List<String> sales = new ArrayList<>();

            Offer currentOffer = Offer.OfferBuilder.create()
               .setUseSlugNameAsInternalSellerId(false)
               .setSellerFullName(MAIN_SELLER)
               .setInternalSellerId(sellerId.toString())
               .setSellerFullName(sellerName)
               .setMainPagePosition(1)
               .setIsBuybox(true)
               .setIsMainRetailer(sellerName != null && sellerName.equalsIgnoreCase(MAIN_SELLER))
               .setSales(sales)
               .setPricing(pricing)
               .build();

            offers.add(currentOffer);
         }
      }
      return offers;
   }

   private Pricing scrapPricing(JSONObject offer, JSONArray installments) throws MalformedPricingException {
      JSONObject priceJson = offer.optJSONObject("price");
      Double priceFrom = priceJson.optDouble("productMPrice");

      Double spotlightPrice = priceJson.optDouble("inCash");
      BankSlip bankSlipPrice = BankSlip.BankSlipBuilder.create().setFinalPrice(spotlightPrice).build();

      CreditCards creditCards = scrapCreditCards(installments, spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setBankSlip(bankSlipPrice)
         .setCreditCards(creditCards)
         .build();

   }

   private CreditCards scrapCreditCards(JSONArray installmentsInfo, Double spotlightPrice) throws MalformedPricingException {
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

   private Installments scrapInstallments(JSONArray installmentsInfo) throws MalformedPricingException {
      Installments installments = new Installments();

      if (installmentsInfo != null) {
         for (Object installmentObject : installmentsInfo) {
            if (!(installmentObject instanceof JSONObject)) continue;
            JSONObject installment = (JSONObject) installmentObject;
            Double value = installment.optDouble("value");
            Integer installmentsNumbers = installment.optInt("number");
            Double finalPrice = installment.optDouble("total");

            installments.add(Installment.InstallmentBuilder.create()
               .setInstallmentNumber(installmentsNumbers)
               .setInstallmentPrice(value)
               .setFinalPrice(finalPrice)
               .build());
         }
      }
      return installments;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("span:contains(Perguntas e Respostas)") != null;
   }

   private RatingsReviews scrapRating(JSONObject product) {
      RatingsReviews ratingReviews = new RatingsReviews();

      Integer totalNumOfEvaluations = JSONUtils.getValueRecursive(product, "rating.rate.count", Integer.class);
      Double avgRating = JSONUtils.getValueRecursive(product, "rating.rate.average", Double.class);

      if (avgRating == null) {
         return ratingReviews;
      }

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
