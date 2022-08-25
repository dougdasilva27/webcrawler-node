package br.com.lett.crawlernode.exceptions;

public class NotFoundProductException extends Exception{
   public NotFoundProductException(){
      super("Not Found product");
   }
   public NotFoundProductException(String message) {
      super(message);
   }
}
