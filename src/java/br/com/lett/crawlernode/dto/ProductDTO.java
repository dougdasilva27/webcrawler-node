package br.com.lett.crawlernode.dto;

import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.SkuStatus;
import br.com.lett.crawlernode.core.session.Session;
import models.Marketplace;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.Seller;
import models.prices.Prices;
import models.pricing.Pricing;

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

   public static Product processCaptureData(Product product) {
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


      }

      return p;
   }
}
