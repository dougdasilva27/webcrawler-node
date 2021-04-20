package br.com.lett.crawlernode.exceptions;

public class MalformedUrlException extends RuntimeException {

   public MalformedUrlException(){
      super("URL incorreta para o market");
   }

   public MalformedUrlException(String msg){
      super(msg);
   }

}
