package br.com.lett.crawlernode.exceptions;

public class RequestMethodNotFoundException extends IllegalArgumentException {

   public RequestMethodNotFoundException() {
      super("Request method not found");
   }

   public RequestMethodNotFoundException(String method) {
      super("Request method not found: " + method);
   }
}
