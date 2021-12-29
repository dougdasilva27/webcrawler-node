package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilCasaDeGeCrawler extends Crawler {
   private static final String SELLER_FULL_NAME = "Casa de Gê";
   private static final Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString());

   public BrasilCasaDeGeCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.JSON);
   }

   @Override
   protected Response fetchResponse() {
      String url = scrapProductUrl();
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setFollowRedirects(true)
         .build();
      return this.dataFetcher.get(session, request);
   }

   private String scrapProductUrl() {
      String productId = null;
      String idUrlRegex = "\\/id\\/([0-9]*)";
      Pattern pattern = Pattern.compile(idUrlRegex, Pattern.MULTILINE);
      final Matcher matcher = pattern.matcher(session.getOriginalURL());

      if(matcher.find()) {
         productId = matcher.group(1);
      }
      return "https://api-marketplace.casadege.com.br/api/collective/client/marketplace/casadege.com.br/core/product/"+ productId + "?id="+ productId;
   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      List<Product> products = new ArrayList<>();

      JSONObject productJson = json.optJSONObject("data");
      if(productJson != null) {
         String name = productJson.optString("name");
         String internalPid = productJson.optString("id");
         String internalId = productJson.optString("code_sku");
         JSONArray imagesJson = productJson.optJSONArray("images");
         List<String> images = imagesJson != null ? CrawlerUtils.scrapImagesListFromJSONArray(imagesJson, "url", null, "", "", session) : null;
         String primaryImage = images != null && !images.isEmpty() ? images.remove(0) : null;
         String description = scrapDescription(productJson);
         CategoryCollection categories = scrapCategories(productJson);
         boolean available = JSONUtils.getIntegerValueFromJSON(productJson, "inventory_quantity", 0) > 0;
         Offers offers = available ? scrapOffers(productJson) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
            .setCategories(categories)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private Offers scrapOffers(JSONObject product) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(product);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .build());

      return offers;
   }

   private Pricing scrapPricing(JSONObject product) throws MalformedPricingException {
      Double spotlightPrice =  JSONUtils.getDoubleValueFromJSON(product, "price", true);
      Double priceFrom =  JSONUtils.getDoubleValueFromJSON(product, "price_compare_at", true);

      Double bankslipDiscount = Double.valueOf(JSONUtils.getIntegerValueFromJSON(product, "discount_percent_boleto", 0));

      if (spotlightPrice.equals(priceFrom)) {
         priceFrom = null;
      }

      CreditCards creditCards = scrapCreditCards(product, spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(BankSlip.BankSlipBuilder.create().setFinalPrice(spotlightPrice).setOnPageDiscount(bankslipDiscount).build())
         .build();
   }

   private CreditCards scrapCreditCards(JSONObject product, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      int installmentsNumber = JSONUtils.getIntegerValueFromJSON(product, "installment_no_fee", 0);
      double installmentsPrice = JSONUtils.getDoubleValueFromJSON(product, "price_installment_no_fee", true);

      if(installmentsNumber == 0 || installmentsPrice == 0d) {
         installmentsNumber = 1;
         installmentsPrice = spotlightPrice;
      }
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(installmentsNumber)
         .setInstallmentPrice(installmentsPrice)
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


   private CategoryCollection scrapCategories(JSONObject product) {
      CategoryCollection categories = new CategoryCollection();

      String brand = JSONUtils.getValueRecursive(product, "salesman.name", String.class, "");
      String category = JSONUtils.getValueRecursive(product, "category.name", String.class, "");

      categories.add(brand);
      categories.add(category);

      return categories;
   }

   private String scrapDescription(JSONObject product) {
      String firstDescription = product.optString("body_description_start");
      String secondDescription = product.optString("body_description_short");
      String thirdDescription = product.optString("body_description_large");

      return firstDescription + "<br>" + secondDescription + "<br>" + thirdDescription;
   }
}
