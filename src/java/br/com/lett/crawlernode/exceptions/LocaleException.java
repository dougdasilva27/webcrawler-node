package br.com.lett.crawlernode.exceptions;

public class LocaleException extends Exception {
   public LocaleException() {
      super("URL não pertence a localidade correta");
   }
}
