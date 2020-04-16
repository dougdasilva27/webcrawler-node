package br.com.lett.crawlernode.core.models;

import java.util.List;

public class Market {

   private int id;
   private String city;
   private String name;
   private boolean crawlerWebdriver;
   private List<String> proxies;
   private List<String> imageProxies;
   private String code;

   /**
    * Default constructor used for testing.
    * 
    * @param marketId
    * @param marketCity
    * @param marketName
    * @param proxies
    */
   public Market() {}

   private Market(MarketBuilder builder) {
      this.id = builder.id;
      this.city = builder.city;
      this.name = builder.name;
      this.code = builder.code;
      this.crawlerWebdriver = builder.crawlerWebdriver;
      this.imageProxies = builder.imageProxies;
      this.proxies = builder.proxies;
   }

   public int getNumber() {
      return id;
   }

   public void setNumber(int number) {
      this.id = number;
   }

   public String getCity() {
      return city;
   }

   public void setCity(String city) {
      this.city = city;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   @Override
   public String toString() {
      return "Market [id=" + id +
            ", city=" + city +
            ", name=" + name +
            ", proxies=" + proxies.toString() +
            ", mustUseWebdriver=" + crawlerWebdriver +
            ", image proxies=" + imageProxies.toString();
   }

   public List<String> getProxies() {
      return proxies;
   }

   public void setProxies(List<String> proxies) {
      this.proxies = proxies;
   }

   public List<String> getImageProxies() {
      return imageProxies;
   }

   public void setImageProxies(List<String> imageProxies) {
      this.imageProxies = imageProxies;
   }

   public boolean mustUseCrawlerWebdriver() {
      return crawlerWebdriver;
   }

   public void setMustUseCrawlerWebdriver(boolean crawlerWebdriver) {
      this.crawlerWebdriver = crawlerWebdriver;
   }

   public int getId() {
      return id;
   }

   public boolean isCrawlerWebdriver() {
      return crawlerWebdriver;
   }

   public String getCode() {
      return code;
   }

   public void setId(int id) {
      this.id = id;
   }

   public void setCrawlerWebdriver(boolean crawlerWebdriver) {
      this.crawlerWebdriver = crawlerWebdriver;
   }

   public void setCode(String code) {
      this.code = code;
   }

   public static class MarketBuilder {

      private int id;
      private String city;
      private String name;
      private boolean crawlerWebdriver;
      private List<String> proxies;
      private List<String> imageProxies;
      private String code;

      public static MarketBuilder create() {
         return new MarketBuilder();
      }

      public MarketBuilder setId(int id) {
         this.id = id;
         return this;
      }

      public MarketBuilder setCity(String city) {
         this.city = city;
         return this;
      }

      public MarketBuilder setName(String name) {
         this.name = name;
         return this;
      }

      public MarketBuilder setCrawlerWebdriver(boolean crawlerWebdriver) {
         this.crawlerWebdriver = crawlerWebdriver;
         return this;
      }

      public MarketBuilder setProxies(List<String> proxies) {
         this.proxies = proxies;
         return this;
      }

      public MarketBuilder setImageProxies(List<String> imageProxies) {
         this.imageProxies = imageProxies;
         return this;
      }

      public MarketBuilder setCode(String code) {
         this.code = code;
         return this;
      }

      public Market build() {
         return new Market(this);
      }
   }
}
