package br.com.lett.crawlernode.util;

public class ScraperInformation {



   private String optionsScraper;
   private String optionsScraperClass;
   private String className;
   private String name;
   private String proxiesMarket;
   private boolean useBrowser;

   public ScraperInformation( String optionsScraper, String optionsScraperClass, String className, String name, boolean useBrowser, String proxiesMarket) {

      this.optionsScraper = optionsScraper;
      this.optionsScraperClass = optionsScraperClass;
      this.className = className;
      this.name = name;
      this.useBrowser = useBrowser;
      this.proxiesMarket = proxiesMarket;
   }

   public String getProxiesMarket() {
      return proxiesMarket;
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
