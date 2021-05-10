package br.com.lett.crawlernode.core.models;

public class Market {

   private int id;
   private String name;
   private String fullName;
   private String code;
   private String firstPartyRegex;
   private boolean useBrowser = false;

   public Market(int id, String name, String fullName, String code, String firstPartyRegex, boolean useBrowser) {
      this.id = id;
      this.name = name;
      this.fullName = fullName;
      this.code = code;
      this.firstPartyRegex = firstPartyRegex;
      this.useBrowser = useBrowser;
   }

   /**
    * Default constructor used for testing.
    * @param marketId
    * @param name
    * @param fullName
    * @param marketCode
    * @param firstPartyRegex
    */
   public Market(
      int marketId,
      String name, String fullName, String marketCode,
      String firstPartyRegex) {

      this.id = marketId;
      this.name = name;
      this.fullName = fullName;
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
         ", code=" + code +
         ", name=" + name +
         ", regex=" + firstPartyRegex +
         "]";
   }

   public String getCode() {
      return code;
   }

   public void setCode(String code) {
      this.code = code;
   }

   public int getId() {
      return id;
   }

   public void setId(int id) {
      this.id = id;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getFirstPartyRegex() {
      return firstPartyRegex;
   }

   public void setFirstPartyRegex(String firstPartyRegex) {
      this.firstPartyRegex = firstPartyRegex;
   }

   public boolean isUseBrowser() {
      return useBrowser;
   }

   public void setUseBrowser(boolean useBrowser) {
      this.useBrowser = useBrowser;
   }

   public String getFullName() {
      return fullName;
   }

   public void setFullName(String fullName) {
      this.fullName = fullName;
   }
}
