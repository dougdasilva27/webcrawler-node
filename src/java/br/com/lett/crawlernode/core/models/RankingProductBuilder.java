package br.com.lett.crawlernode.core.models;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class RankingProductBuilder {

   private List<Long> processedIds = new ArrayList<>();
   private Boolean isSponsored = false;
   private Boolean isAvailable;
   private int position;
   private Integer priceInCents;
   private int marketId;
   private int pageNumber = 0;
   private String url;
   private String internalPid;
   private String internalId;
   private String name;
   private String imageUrl;
   private String keyword;
   private String screenshot;
   private Timestamp timestamp;

   public static RankingProductBuilder create() {
      return new RankingProductBuilder();
   }

   public RankingProductBuilder setIsSponsored(Boolean isSponsored) {
      this.isSponsored = isSponsored;
      return this;
   }

   public RankingProductBuilder setAvailability(boolean isAvailable) {
      this.isAvailable = isAvailable;
      return this;
   }

   public RankingProductBuilder setUrl(String url) {
      this.url = url;
      return this;
   }

   public RankingProductBuilder setInternalId(String internalId) {
      this.internalId = internalId;
      return this;
   }

   public RankingProductBuilder setInternalPid(String internalPid) {
      this.internalPid = internalPid;
      return this;
   }

   public RankingProductBuilder setName(String name) {
      this.name = name;
      return this;
   }

   public RankingProductBuilder setPriceInCents(Integer priceInCents) {
      this.priceInCents = priceInCents;
      return this;
   }

   public RankingProductBuilder setPosition(int position) {
      this.position = position;
      return this;
   }

   public RankingProductBuilder setMarketId(int marketId) {
      this.marketId = marketId;
      return this;
   }

   public RankingProductBuilder setPageNumber(int pageNumber) {
      this.pageNumber = pageNumber;
      return this;
   }

   public RankingProductBuilder setKeyword(String keyword) {
      this.keyword = keyword;
      return this;
   }

   public RankingProductBuilder setImageUrl(String imageUrl) {
      this.imageUrl = imageUrl;
      return this;
   }

   public RankingProductBuilder setScreenshot(String screenshot) {
      this.screenshot = screenshot;
      return this;
   }

   public RankingProducts build() throws MalformedProductException {
      RankingProducts product = new RankingProducts();

      product.setUrl(this.url);
      product.setInternalId(this.internalId);
      product.setInteranlPid(this.internalPid);
      product.setName(this.name);
      product.setImageUrl(this.imageUrl);
      product.setKeyword(this.keyword);
      product.setPageNumber(this.pageNumber);
      product.setIsAvailable(this.isAvailable);
      product.setIsSponsored(this.isSponsored);
      product.setPriceInCents(this.priceInCents);
      product.setMarketId(this.marketId);
      product.setScreenshot(this.screenshot);
      product.setProcessedIds(this.processedIds);
      product.setPosition(this.position);

      // Timestamp is only created here, there is no public method to set timestamp
      String nowISO = new DateTime(DateTimeZone.forID("America/Sao_Paulo")).toString("yyyy-MM-dd HH:mm:ss.mmm");
      Timestamp ts = Timestamp.valueOf(nowISO);
      product.setTimestamp(ts);

      check();

      return product;
   }

   private void check() throws MalformedProductException {
      if (this.internalId == null || this.internalId.isEmpty()) {
         if (this.internalPid == null || this.internalPid.isEmpty()){
            throw new MalformedProductException("Both id's can't be null or empty");
         }
      }

      if (this.url == null || this.url.isEmpty()) {
         throw new MalformedProductException("Product url can't be null");
      }

      if (this.name == null || this.name.isEmpty()) {
         throw new MalformedProductException("Product name can't be null");
      }

      if (this.isAvailable && this.priceInCents == 0) {
         throw new MalformedProductException("Price can't be 0 when product is available");
      }
   }

}
