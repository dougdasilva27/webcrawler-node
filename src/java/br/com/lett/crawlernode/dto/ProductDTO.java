package br.com.lett.crawlernode.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.SkuStatus;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.Logging;
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

   private static final Logger logger = LoggerFactory.getLogger(ProductDTO.class);

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

         Offers offers = p.getOffers();
         for (Offer o : offers.getOffersList()) {
            List<String> sales = o.getSales();

            if (sales != null) {
               boolean hasNullValue = false;
               for (String sale : sales) {
                  if (sale == null) {
                     hasNullValue = true;
                     break;
                  }
               }

               if (hasNullValue) {
                  o.setSales(new ArrayList<>());
                  Logging.printLogWarn(logger, session, "SALES CANNOT HAVE VALUE NULL!");
               }
            }
         }

         RatingsReviews r = new RatingsReviews();

         if (session.getInternalId() != null) {
            r.setInternalId(p.getInternalId());
            r.setUrl(p.getUrl());
            r.setMarketId(session.getMarket().getNumber());

            if (product.getRatingReviews() != null) {
               r.setAverageOverallRating(p.getRatingReviews().getAverageOverallRating());
               r.setTotalRating(p.getRatingReviews().getTotalReviews());
               r.setTotalWrittenReviews(p.getRatingReviews().getTotalWrittenReviews());
               r.setAdvancedRatingReview(p.getRatingReviews().getAdvancedRatingReview());
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
               } else if (!offer.getIsMainRetailer()) {
                  marketplace.add(new Seller(offer));
               }
            }
         }

         p.setMarketplace(marketplace);
      } else if (product.getAvailable() || (product.getMarketplace() != null && product.getMarketplace().size() > 0)) {
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

         Marketplace mkt = product.getMarketplace();
         if (mkt != null) {
            for (int i = 0; i < mkt.size(); i++) {
               Seller seller = mkt.get(i);

               offers.add(OfferBuilder.create()
                     .setUseSlugNameAsInternalSellerId(true)
                     .setSellerFullName(seller.getName())
                     .setIsBuybox(mkt.size() > 1 || (product.getAvailable() && mkt.size() > 0))
                     .setMainPagePosition(i + 2)
                     .setIsMainRetailer(false)
                     .setPricing(pricesToPricing(seller.getPrices(), MathUtils.normalizeTwoDecimalPlaces(seller.getPrice().floatValue())))
                     .build());
            }
         }

         p.setOffers(offers);
      } else if (product.getOffers() == null) {
         p.setOffers(new Offers());
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
