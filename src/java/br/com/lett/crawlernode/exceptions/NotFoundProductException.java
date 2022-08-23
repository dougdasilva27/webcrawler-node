package br.com.lett.crawlernode.exceptions;

public class NotFoundProductException  extends Exception{
   public NotFoundProductException(){
      super("Malformed model");
   }
   public NotFoundProductException(String message) {
      super(message);
   }
}
