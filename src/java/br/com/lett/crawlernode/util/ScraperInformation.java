package br.com.lett.crawlernode.util;

public class ScraperInformation {


   private Long marketId;
   private String code;
   private String regex;
   private String fullName;
   private String optionsScraper;
   private String optionsScraperClass;
   private String className;
   private String name;
   private String proxiesImages;
   private boolean useBrowser;

   public ScraperInformation(Long marketId, String code, String regex, String fullName, String optionsScraper, String optionsScraperClass, String className, String name, boolean useBrowser) {
      this.marketId = marketId;
      this.code = code;
      this.regex = regex;
      this.fullName = fullName;
      this.optionsScraper = optionsScraper;
      this.optionsScraperClass = optionsScraperClass;
      this.className = className;
      this.name = name;
      this.useBrowser = useBrowser;
   }

   public ScraperInformation(Long marketId, String code, String proxiesImages) {
      this.marketId = marketId;
      this.code = code;
      this.proxiesImages = proxiesImages;

   }

   public String getProxiesImages() {
      return proxiesImages;
   }

   public void setProxiesImages(String proxiesImages) {
      this.proxiesImages = proxiesImages;
   }

   public Long getMarketId() {
      return marketId;
   }

   public void setMarketId(Long marketId) {
      this.marketId = marketId;
   }

   public String getCode() {
      return code;
   }

   public String getRegex() {
      return regex;
   }

   public String getFullName() {
      return fullName;
   }

   public String getOptionsScraper() {
      return optionsScraper;
   }

   public String getOptionsScraperClass() {
      return optionsScraperClass;
   }

   public String getName() {
      return name;
   }

   public String getClassName() {
      return className;
   }

   public boolean isUseBrowser() {
      return useBrowser;
   }
}
