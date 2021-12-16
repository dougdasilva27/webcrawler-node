package br.com.lett.crawlernode.exceptions;

public class CrawlerSeedRequestException extends Exception {

   public CrawlerSeedRequestException(String message){
      super("Seed failed: " + message);
   }


}
