package br.com.lett.crawlernode.exceptions;

public class AuthenticationException extends Exception{

   public AuthenticationException(String message) {
      super("Authentication failed: " + message);
   }
}
