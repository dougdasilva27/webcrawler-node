package br.com.lett.crawlernode.exceptions;

public class SeedCrawlerSessionException extends Exception {

   public SeedCrawlerSessionException(String message){
      super("Seed failed: " + message);
   }


}
