package br.com.lett.crawlernode.crawlers.corecontent.brasil;

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
         String objectScript = CrawlerUtils.scrapScriptFromHtml(doc, "[type=\"application/ld+json\"]");
         JSONArray jsonObject = new JSONArray(objectScript);
         JSONObject productJson = (JSONObject) jsonObject.get(0);

         String aaa = CrawlerUtils.scrapScriptFromHtml(doc, "#__NEXT_DATA__");
         JSONArray jsonObject2 = new JSONArray(aaa);
         JSONObject productJson2 = (JSONObject) jsonObject2.get(0);

         String internalId = productJson.getString("sku");
         String name = productJson.getString("name") + " - " + productJson.getString("color");
         String description = productJson.getString("description");
         List<String> images = JSONUtils.jsonArrayToStringList(productJson.optJSONArray("image"));
         String primaryImage = !images.isEmpty() ? images.remove(0) : null;
         List<String> secondaryImages = !images.isEmpty() ? images : null;
         Boolean available = scrapAvailability(productJson);
         Offers offers = available ? scrapOffers(productJson2) : new Offers();

//         RatingsReviews ratingsReviews = scrapRating(internalId, doc);
//         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb li", true);

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setOffers(offers)
//            .setCategory1(categories.getCategory(0))
//            .setCategory2(categories.getCategory(1))
//            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
//            .setRatingReviews(ratingsReviews)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private Offers scrapOffers(JSONObject product) throws MalformedPricingException, OfferException {

      Offers offers = new Offers();

      JSONArray offersJson = JSONUtils.getJSONArrayValue(product, "offers");

      for (Object e: offersJson){

         JSONObject offer = (JSONObject) e;

         Pricing pricing = scrapPricing(offer);
         List<String> sales = new ArrayList<>();
         JSONObject seller = offer.optJSONObject("seller");
         String sellerName = seller.optString("name");

         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setInternalSellerId(null)
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

   private Pricing scrapPricing(JSONObject offer) throws MalformedPricingException {

      JSONObject price = offer.optJSONObject("price");
      Double priceFrom = null;

      Double spotlightPrice = price.optDouble("inCash");
      BankSlip bankSlipPrice = BankSlip.BankSlipBuilder.create().setFinalPrice(spotlightPrice).build();

      JSONObject installmentsInfo = offer.optJSONObject("installmentToDisplay");

      CreditCards creditCards = scrapCreditCards(installmentsInfo, spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setBankSlip(bankSlipPrice)
         .setCreditCards(creditCards)
         .build();

   }

   private CreditCards scrapCreditCards(JSONObject priceInfo, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = scrapInstallments(priceInfo , spotlightPrice);
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

   private Installments scrapInstallments(JSONObject installmentsInfo, Double spotlightPrice) throws MalformedPricingException {
      Installments installments = new Installments();

      Double value;
      int installmentsNumbers;

      if(installmentsInfo != null){
         value = installmentsInfo.optDouble("value");
         installmentsNumbers = installmentsInfo.optInt("number");

         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(installmentsNumbers)
            .setInstallmentPrice(value)
            .setFinalPrice(spotlightPrice)
            .build());

      }
      return installments;
   }

   private boolean isProductPage(Document doc) {
      return !doc.select("body.black-friday-theme").isEmpty();
   }

   private RatingsReviews scrapRating(String internalId, Document doc) {
      TrustvoxRatingCrawler trustVox = new TrustvoxRatingCrawler(session, "85050", logger);
      return trustVox.extractRatingAndReviews(internalId, doc, dataFetcher);
   }

   private Boolean scrapAvailability(JSONObject product) {
      String available = JSONUtils.getValueRecursive(product, "offers.0.availability", String.class);

      if (available.equals("https://schema.org/InStock")) {
         return true;
      }
      return false;
   }

}
