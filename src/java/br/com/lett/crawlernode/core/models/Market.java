package br.com.lett.crawlernode.core.models;

import java.util.List;

public class Market {

   private int id;
   private String city;
   private String name;
   private String fullName;
   private String code;
   private boolean crawlerWebdriver;
   private List<String> proxies;
   private List<String> imageProxies;
   private String firstPartyRegex;

   /**
    * Default constructor used for testing.
    * 
    * @param marketId
    * @param marketCity
    * @param marketName
    * @param proxies
    */
   public Market(
                 int marketId,
                 String marketCity,
                 String marketName,
                 String marketCode,
                 String marketFullName,
                 List<String> proxies,
                 List<String> imageProxies,
                 String firstPartyRegex) {

      this.id = marketId;
      this.city = marketCity;
      this.name = marketName;
      this.code = marketCode;
      this.fullName = marketFullName;
      this.proxies = proxies;
      this.imageProxies = imageProxies;
      this.firstPartyRegex = firstPartyRegex;
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
            ", code=" + code +
            ", city=" + city +
            ", fullName=" + fullName +
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

   public String getFullName() {
      return fullName;
   }

   public String getCode() {
      return code;
   }

   public void setFullName(String fullName) {
      this.fullName = fullName;
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