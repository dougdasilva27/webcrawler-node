package br.com.lett.crawlernode.exceptions;

public class NotMarketUrlException extends Exception {

   public NotMarketUrlException(){
      super("URL incorreta para o market");
   }

   public NotMarketUrlException(String msg){
      super(msg);
   }

}
