package br.com.lett.crawlernode.core.models;

import java.util.Collection;
import java.util.List;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.DateUtils;
import models.Marketplace;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.Seller;
import models.prices.Prices;
import models.pricing.Pricing;


public class ProductBuilder {

   private String url;
   private String internalId;
   private String internalPid;
   private String name;

   @Deprecated // Use Offers instead
   private Float price;

   @Deprecated // Use Offers instead
   private Prices prices;

   @Deprecated // Use Offers instead
   private Marketplace marketplace;

   @Deprecated // Use Offers instead
   private boolean available;

   private String category1;
   private String category2;
   private String category3;
   private String primaryImage;
   private String secondaryImages;
   private String description;
   private Integer stock;
   private List<String> eans;
   private Offers offers;
   private RatingsReviews ratingReviews;

   public static ProductBuilder create() {
      return new ProductBuilder();
   }

   public ProductBuilder setUrl(String url) {
      this.url = url;
      return this;
   }

   public ProductBuilder setInternalId(String internalId) {
      this.internalId = internalId;
      return this;
   }

   public ProductBuilder setInternalPid(String internalPid) {
      this.internalPid = internalPid;
      return this;
   }

   public ProductBuilder setName(String name) {
      this.name = name;
      return this;
   }

   @Deprecated // Use Offers instead
   public ProductBuilder setPrice(Float price) {
      this.price = price;
      return this;
   }

   @Deprecated // Use Offers instead
   public ProductBuilder setAvailable(boolean available) {
      this.available = available;
      return this;
   }

   public ProductBuilder setCategories(Collection<String> categories) {
      int i = 1;
      for (String category : categories) {
         if (i == 1)
            setCategory1(category);
         else if (i == 2)
            setCategory2(category);
         else if (i == 3)
            setCategory3(category);
         i++;
      }
      return this;
   }

   public ProductBuilder setCategory1(String category1) {
      this.category1 = category1;
      return this;
   }

   public ProductBuilder setCategory2(String category2) {
      this.category2 = category2;
      return this;
   }

   public ProductBuilder setCategory3(String category3) {
      this.category3 = category3;
      return this;
   }

   public ProductBuilder setPrimaryImage(String primaryImage) {
      this.primaryImage = primaryImage;
      return this;
   }

   public ProductBuilder setSecondaryImages(String secondaryImages) {
      this.secondaryImages = secondaryImages;
      return this;
   }

   public ProductBuilder setDescription(String description) {
      this.description = description;
      return this;
   }

   @Deprecated // Use Offers instead
   public ProductBuilder setMarketplace(Marketplace marketplace) {
      this.marketplace = marketplace;
      return this;
   }

   public ProductBuilder setStock(Integer stock) {
      this.stock = stock;
      return this;
   }

   @Deprecated // Use Offers instead
   public ProductBuilder setPrices(Prices prices) {
      this.prices = prices;
      return this;
   }

   public ProductBuilder setEans(List<String> eans) {
      this.eans = eans;
      return this;
   }

   public ProductBuilder setOffers(Offers offers) {
      this.offers = offers;
      return this;
   }

   public ProductBuilder setRatingReviews(RatingsReviews ratingReviews) {
      this.ratingReviews = ratingReviews;
      return this;
   }

   public Product build() throws MalformedProductException {
      Product product = new Product();

      product.setUrl(this.url);
      product.setInternalId(this.internalId);
      product.setInternalPid(this.internalPid);
      product.setName(this.name);
      product.setCategory1(this.category1);
      product.setCategory2(this.category2);
      product.setCategory3(this.category3);
      product.setPrimaryImage(this.primaryImage);
      product.setSecondaryImages(this.secondaryImages);
      product.setDescription(this.description);
      product.setStock(this.stock);
      product.setEans(this.eans);
      product.setRatingReviews(this.ratingReviews);

      product.setOffers(this.offers);

      if (this.offers != null) {
         this.available = false;
         this.prices = new Prices();
         this.marketplace = new Marketplace();
         this.price = null;

         for (Offer offer : this.offers.getOffersList()) {
            if (offer.getIsMainRetailer()) {
               Pricing pricing = offer.getPricing();
               this.available = true;
               this.prices = new Prices(pricing);
               this.price = pricing.getSpotlightPrice().floatValue();
            } else {
               this.marketplace.add(new Seller(offer));
            }
         }
      }

      product.setAvailable(this.available);
      product.setPrices(this.prices);
      product.setPrice(this.price);
      product.setMarketplace(this.marketplace);

      // Timestamp is only created here, there is no public method to set timestamp
      product.setTimestamp(DateUtils.newTimestamp());

      check();

      return product;
   }

   private void check() throws MalformedProductException {
      if (this.internalId == null || this.internalId.isEmpty()) {
         throw new MalformedProductException("InternalId can't be null or empty");
      }

      if (this.name == null || this.name.isEmpty()) {
         throw new MalformedProductException("Name", this.name);
      }

      if (this.available && (this.prices == null || this.prices.isEmpty())) {
         throw new MalformedProductException("Prices can't be null or empty when product is available");
      }

      if (this.available && this.price == null) {
         throw new MalformedProductException("Price can't be null when product is available");
      }
   }

}