package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.*;

/**
 * Date: 2019-08-28
 *
 * @author gabriel
 */
public abstract class ArgentinaCarrefoursuper extends CarrefourCrawler {

   private static final String SELLER_FULL_NAME = "CARREFOUR";

   protected ArgentinaCarrefoursuper(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.APACHE);
   }

   @Override
   protected String getHomePage() {
      return "https://www.carrefour.com.ar/";
   }

   @Override
   protected List<String> getMainSellersNames() {
      return Collections.singletonList(SELLER_FULL_NAME);
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }

   protected JSONObject crawlProductApi(String internalPid, String parameters) {
      JSONObject productApi = new JSONObject();

      String url = homePage + "api/catalog_system/pub/products/search?fq=productId:" + internalPid + (parameters == null ? "" : parameters);

      String page = fetchPage(url);
      JSONArray array = CrawlerUtils.stringToJsonArray(page);

      if (!array.isEmpty()) {
         productApi = array.optJSONObject(0) == null ? new JSONObject() : array.optJSONObject(0);
      }

      return productApi;
   }

   @Override
   protected Offers scrapOffer(Document doc, JSONObject jsonSku, String internalId, String internalPid) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();

      JSONArray sellers = jsonSku.optJSONArray("sellers");
      if (sellers != null) {
         int position = 1;
         for (Object o : sellers) {
            JSONObject offerJson = o instanceof JSONObject ? (JSONObject) o
               : new JSONObject();
            JSONObject commertialOffer = offerJson.optJSONObject("commertialOffer");

            String seller = offerJson.optString("sellerName");
            String sellerFullName = seller != null ? seller : "Carrefour";
            boolean isDefaultSeller = offerJson.optBoolean("sellerDefault", true);

            if (commertialOffer != null) {
               int stock = commertialOffer.optInt("AvailableQuantity");

               if (stock > 0) {

                  boolean isBuyBox = sellers.length() > 1;
                  boolean isMainRetailer = isMainRetailer(sellerFullName);

                  Pricing pricing = scrapPricing(commertialOffer);
                  List<String> sales = isDefaultSeller ? scrapSales(doc, offerJson, internalId, internalPid, pricing) : new ArrayList<>();

                  offers.add(Offer.OfferBuilder.create()
                     .setUseSlugNameAsInternalSellerId(true)
                     .setSellerFullName(sellerFullName)
                     .setMainPagePosition(position)
                     .setIsBuybox(isBuyBox)
                     .setIsMainRetailer(isMainRetailer)
                     .setPricing(pricing)
                     .setSales(sales)
                     .build());

                  position++;
               }
            }
         }
      }

      return offers;
   }


   protected Pricing scrapPricing(JSONObject comertial) throws MalformedPricingException {
      Double priceFrom = JSONUtils.getDoubleValueFromJSON(comertial, "ListPrice", true);
      Double spotlightPrice = getSpotlightPrice(comertial);
      if (priceFrom.equals(spotlightPrice)){
         priceFrom = null;
      }

      CreditCards creditCards = scrapCreditCards(comertial, spotlightPrice);
      BankSlip bankSlip = scrapBankSlip(spotlightPrice);


      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();
   }

   protected Double scrapDiscount(JSONObject comertial) {
      Double discount = null;

      JSONArray teasers = comertial.optJSONArray("Teasers");
      if (teasers != null && !teasers.isEmpty()) {
         for (Object o : teasers) {
            JSONObject teaser = (JSONObject) o;

            discount = scrapPaymentDiscount(teaser);

         }

      }
      return discount;

   }

   protected BankSlip scrapBankSlip(Double spotlightPrice) throws MalformedPricingException {
      return BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();
   }

   protected CreditCards scrapCreditCards(JSONObject comertial, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      JSONObject paymentOptions = comertial.optJSONObject("PaymentOptions");
      if (paymentOptions != null) {
         JSONArray cardsArray = paymentOptions.optJSONArray("installmentOptions");
         if (cardsArray != null) {
            for (Object o : cardsArray) {
               JSONObject cardJson = (JSONObject) o;
               String paymentName = cardJson.optString("paymentName");
               JSONArray installmentsArray = cardJson.optJSONArray("installments");

               if (!installmentsArray.isEmpty() && !paymentName.toLowerCase().contains("boleto")) {
                  Installments installments = new Installments();
                  for (Object object : installmentsArray) {
                     JSONObject installmentJson = (JSONObject) object;
                     Double value;
                     int installmentNumber = installmentJson.optInt("count");
                     if (installmentNumber == 1) {
                        value = spotlightPrice;
                     } else {
                        value = installmentJson.optDouble("value") / 100d;
                     }
                     Double totalValue = installmentJson.optDouble("total") / 100d;
                     Double interest = installmentJson.optDouble("interestRate");

                     installments.add(setInstallment(installmentNumber, value, interest, totalValue, null));
                  }

                  String cardBrand = null;
                  for (Card card : Card.values()) {
                     if (card.toString().toLowerCase().contains(paymentName.toLowerCase())) {
                        cardBrand = card.toString();
                        break;
                     }
                  }

                  boolean isShopCard = false;
                  if (cardBrand == null) {
                     for (String sellerName : mainSellersNames) {
                        if ((paymentName.equalsIgnoreCase(storeCard)) ||
                           paymentName.toLowerCase().contains(sellerName.toLowerCase())) {
                           isShopCard = true;
                           cardBrand = paymentName;
                           break;
                        }
                     }
                  }

                  if (cardBrand != null) {
                     creditCards.add(CreditCard.CreditCardBuilder.create()
                        .setBrand(cardBrand)
                        .setInstallments(installments)
                        .setIsShopCard(isShopCard)
                        .build());
                  }
               }
            }
         }
      }

      return creditCards;
   }

   protected Installment setInstallment(Integer installmentNumber, Double value, Double interests, Double totalValue, Double discount) throws MalformedPricingException {
      if (interests != null && (interests.isNaN() || interests.isInfinite())) {
         interests = null;
      }

      return Installment.InstallmentBuilder.create()
         .setInstallmentNumber(installmentNumber)
         .setInstallmentPrice(value)
         .setAmOnPageInterests(interests)
         .setFinalPrice(totalValue)
         .setOnPageDiscount(discount)
         .build();
   }

   private Double getSpotlightPrice(JSONObject commertial) {
      Double spotlightPrice = null;

      JSONArray installments = commertial.optJSONArray("Installments");
      for (Object o : installments) {
         JSONObject installment = (JSONObject) o;
         String paymentName = installment.optString("Name");
         if (paymentName.contains("vista")) {
            spotlightPrice = installment.optDouble("Value");
            break;
         }

      }

      return spotlightPrice;
   }


}
