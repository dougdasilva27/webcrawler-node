package br.com.lett.crawlernode.core.fetcher;

public enum FetchMode {

   STATIC, APACHE, WEBDRIVER, FETCHER, JAVANET, JSOUP, HTTPCLIENT, MIRANHA;

   @Override
   public String toString() {
      switch (this) {
         case STATIC:
            return STATIC.name();
         case APACHE:
            return APACHE.name();
         case WEBDRIVER:
            return WEBDRIVER.name();
         case FETCHER:
            return FETCHER.name();
         case JAVANET:
            return JAVANET.name();
         case JSOUP:
            return JSOUP.name();
         case HTTPCLIENT:
            return HTTPCLIENT.name();
         case MIRANHA:
            return MIRANHA.name();
         default:
            return "";
      }
   }

}
