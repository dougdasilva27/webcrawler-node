package br.com.lett.crawlernode.core.models;

import java.util.List;

public class Market {

   private int id;
   private String code;
   private String firstPartyRegex;

   /**
    * Default constructor used for testing.
    * 
    * @param marketId
    * @param marketCode
    * @param firstPartyRegex
    */
   public Market(
      int marketId,
      String marketCode,
      String firstPartyRegex) {

      this.id = marketId;
      this.code = marketCode;
      this.firstPartyRegex = firstPartyRegex;
   }

   public int getNumber() {
      return id;
   }

   public void setNumber(int number) {
      this.id = number;
   }

   @Override
   public String toString() {
      return "Market [id=" + id +
            ", code=" + code;
   }

   public String getCode() {
      return code;
   }

   public void setCode(String code) {
      this.code = code;
   }

   public String getFirstPartyRegex() {
      return firstPartyRegex;
   }

   public void setFirstPartyRegex(String firstPartyRegex) {
      this.firstPartyRegex = firstPartyRegex;
   }
}
