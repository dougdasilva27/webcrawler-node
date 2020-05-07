package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VtexConfig {

   private String homePage;
   private List<String> mainSellerNames;
   private boolean hasBankTicket = true;
   private boolean usePriceAPI = true;
   private Integer bankDiscount;
   private List<CardsInfo> cards;
   private List<String> sales = new ArrayList<>();
   private boolean salesIsCalculated = false;

   public VtexConfig(VtexConfigBuilder build) {
      this.homePage = build.homePage;
      this.mainSellerNames = build.mainSellerNames;
      this.hasBankTicket = build.hasBankTicket;
      this.setUsePriceAPI(build.usePriceAPI);
      this.bankDiscount = build.bankDiscount;
      this.cards = build.cards;
      this.sales = build.sales;
      this.salesIsCalculated = build.salesIsCalculated;
   }

   public String getHomePage() {
      return homePage;
   }

   public List<String> getMainSellerNames() {
      return mainSellerNames;
   }

   public boolean isHasBankTicket() {
      return hasBankTicket;
   }

   public boolean isUsePriceAPI() {
      return usePriceAPI;
   }

   public void setUsePriceAPI(boolean usePriceAPI) {
      this.usePriceAPI = usePriceAPI;
   }

   public Integer getBankDiscount() {
      return bankDiscount;
   }

   public List<CardsInfo> getCards() {
      return cards;
   }

   public CardsInfo getCardInfoByBrand(String brand) {
      CardsInfo cardsInfo = null;

      if (this.cards != null) {
         for (CardsInfo c : cards) {
            if (c.getBrand().equalsIgnoreCase(brand)) {
               cardsInfo = c;
               break;
            }
         }
      }

      return cardsInfo;
   }

   public void setHomePage(String homePage) {
      this.homePage = homePage;
   }

   public void setMainSellerNames(List<String> mainSellerNames) {
      this.mainSellerNames = mainSellerNames;
   }

   public void setHasBankTicket(boolean hasBankTicket) {
      this.hasBankTicket = hasBankTicket;
   }

   public void setBankDiscount(Integer bankDiscount) {
      this.bankDiscount = bankDiscount;
   }

   public void setCards(List<CardsInfo> cards) {
      this.cards = cards;
   }

   public List<String> getSales() {
      return sales;
   }

   public void setSales(List<String> sales) {
      this.sales = sales;
   }

   public boolean isSalesIsCalculated() {
      return salesIsCalculated;
   }

   public void setSalesIsCalculated(boolean salesIsCalculated) {
      this.salesIsCalculated = salesIsCalculated;
   }

   public static class VtexConfigBuilder {
      private String homePage;
      private List<String> mainSellerNames;
      private boolean hasBankTicket = true;
      private boolean usePriceAPI = true;
      private Integer bankDiscount;
      private List<CardsInfo> cards;
      private List<String> sales = new ArrayList<>();
      private boolean salesIsCalculated = false;

      public static VtexConfigBuilder create() {
         return new VtexConfigBuilder();
      }

      public VtexConfigBuilder setHomePage(String homePage) {
         this.homePage = homePage;
         return this;
      }

      public VtexConfigBuilder setMainSellerNames(List<String> mainSellerNames) {
         this.mainSellerNames = mainSellerNames;
         return this;
      }

      public VtexConfigBuilder setHasBankTicket(boolean hasBankTicket) {
         this.hasBankTicket = hasBankTicket;
         return this;
      }

      public VtexConfigBuilder setUsePriceAPI(boolean usePriceAPI) {
         this.usePriceAPI = usePriceAPI;
         return this;
      }

      public VtexConfigBuilder setSalesIsCalculated(boolean salesIsCalculated) {
         this.salesIsCalculated = salesIsCalculated;
         return this;
      }

      public VtexConfigBuilder setBankDiscount(Integer bankDiscount) {
         this.bankDiscount = bankDiscount;
         return this;
      }

      public VtexConfigBuilder setCards(List<CardsInfo> cards) {
         this.cards = cards;
         return this;
      }

      public VtexConfigBuilder setSales(List<String> sales) {
         this.sales = sales;
         return this;
      }

      public VtexConfig build() {
         return new VtexConfig(this);
      }
   }

   public static class CardsInfo {
      private String brand;
      private Map<Integer, Integer> installmentsDiscounts;
      private boolean isShopCard;

      public CardsInfo(CardsInfoBuilder build) {
         this.brand = build.brand;
         this.installmentsDiscounts = build.installmentsDiscounts;
         this.isShopCard = build.isShopCard;
      }

      public String getBrand() {
         return brand;
      }

      public Map<Integer, Integer> getInstallmentsDiscounts() {
         return installmentsDiscounts;
      }

      public boolean isShopCard() {
         return isShopCard;
      }

      public void setCard(String card) {
         this.brand = card;
      }

      public void setInstallmentsDiscounts(Map<Integer, Integer> installmentsDiscounts) {
         this.installmentsDiscounts = installmentsDiscounts;
      }

      public void setShopCard(boolean isShopCard) {
         this.isShopCard = isShopCard;
      }

      public static class CardsInfoBuilder {
         private String brand;
         private Map<Integer, Integer> installmentsDiscounts;
         private boolean isShopCard;

         public static CardsInfoBuilder create() {
            return new CardsInfoBuilder();
         }

         public CardsInfoBuilder setCard(String card) {
            this.brand = card;
            return this;
         }

         public CardsInfoBuilder setInstallmentsDiscounts(Map<Integer, Integer> installmentsDiscounts) {
            this.installmentsDiscounts = installmentsDiscounts;
            return this;
         }

         public CardsInfoBuilder setShopCard(boolean isShopCard) {
            this.isShopCard = isShopCard;
            return this;
         }

         public CardsInfo build() {
            return new CardsInfo(this);
         }
      }
   }
}
