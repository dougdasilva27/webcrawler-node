package br.com.lett.crawlernode.dto;

import java.util.Map;
import java.util.Map.Entry;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.SkuStatus;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.MathUtils;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Marketplace;
import models.Offer;
import models.Offer.OfferBuilder;
import models.Offers;
import models.RatingsReviews;
import models.Seller;
import models.prices.Prices;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;

public class ProductDTO {

   public static Product convertProductToKinesisFormat(Product product, Session session) {
      Product p = new Product();

      if (product.isVoid()) {
         p.setInternalId(session.getInternalId());
         p.setInternalPid(product.getInternalPid());
         p.setAvailable(false);
         p.setStatus(SkuStatus.VOID);
      } else {
         p = product.clone();
         if (p.getAvailable()) {
            p.setStatus(SkuStatus.AVAILABLE);
         } else {
            if (p.getMarketplace() != null && p.getMarketplace().size() > 0) {
               p.setStatus(SkuStatus.MARKETPLACE_ONLY);
            } else {
               p.setStatus(SkuStatus.UNAVAILABLE);
            }
         }

         RatingsReviews r = new RatingsReviews();

         if (session.getInternalId() != null) {
            r.setInternalId(session.getInternalId());
            r.setUrl(session.getOriginalURL());
            r.setMarketId(session.getMarket().getNumber());

            if (product.getRatingReviews() != null) {
               r.setAverageOverallRating(product.getRatingReviews().getAverageOverallRating());
               r.setTotalRating(product.getRatingReviews().getTotalReviews());
               r.setTotalWrittenReviews(product.getRatingReviews().getTotalWrittenReviews());
            }
         }

         p.setRatingReviews(r);
      }

      p.setUrl(session.getOriginalURL());
      p.setMarketId(session.getMarket().getNumber());

      return p;
   }

   public static Product processCaptureData(Product product, Session session) throws OfferException, MalformedPricingException {
      Product p = product.clone();

      if (product.getOffers() != null) {
         Marketplace marketplace = new Marketplace();
         for (Offer offer : product.getOffers().getOffersList()) {
            if (offer.getPricing() != null) {
               if (offer.getIsMainRetailer() && p.getPrice() == null) {
                  Pricing pricing = offer.getPricing();
                  p.setAvailable(true);
                  p.setPrices(new Prices(pricing));
                  p.setPrice(pricing.getSpotlightPrice().floatValue());
               } else {
                  marketplace.add(new Seller(offer));
               }
            }
         }

         p.setMarketplace(marketplace);
      } else if (product.getPrices() != null && !product.getPrices().isEmpty()) {
         Offers offers = new Offers();

         if (product.getAvailable()) {
            offers.add(OfferBuilder.create()
                  .setInternalSellerId(session.getMarket().getName())
                  .setSellerFullName(session.getMarket().getFullName())
                  .setIsBuybox(false)
                  .setMainPagePosition(1)
                  .setIsMainRetailer(true)
                  .setPricing(pricesToPricing(product.getPrices(), product.getPrice()))
                  .build());
         }

         p.setOffers(offers);
      }

      return p;
   }

   public static Pricing pricesToPricing(Prices prices, Float price) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      for (Card card : Card.values()) {
         Map<Integer, Double> installmentsMap = prices.getCardPaymentOptions(card.toString());
         if (installmentsMap != null) {
            Installments installments = new Installments();

            for (Entry<Integer, Double> entry : installmentsMap.entrySet()) {
               installments.add(InstallmentBuilder.create()
                     .setInstallmentNumber(entry.getKey())
                     .setInstallmentPrice(entry.getValue())
                     .build());
            }

            creditCards.add(CreditCardBuilder.create()
                  .setBrand(card.toString())
                  .setInstallments(installments)
                  .setIsShopCard(card.toString().equalsIgnoreCase(Card.SHOP_CARD.toString()))
                  .build());
         }
      }

      return PricingBuilder.create()
            .setSpotlightPrice(MathUtils.normalizeTwoDecimalPlaces(price.doubleValue()))
            .setCreditCards(creditCards)
            .setBankSlip(
                  BankSlipBuilder.create()
                        .setFinalPrice(prices.getBankTicketPrice())
                        .build()
            )
            .build();
   }
}
