package br.com.lett.crawlernode.exceptions;

/**
 * When a proxy service is not found.
 * 
 * @author Samir Leao
 *
 */
public class MalformedProductException extends Exception {

   private static final long serialVersionUID = 1L;

   public MalformedProductException(String message) {
      super(message);
   }

   public MalformedProductException(String field, Object value) {
      super("Field " + field + " cannot have this value [" + value + "]");
   }

}
