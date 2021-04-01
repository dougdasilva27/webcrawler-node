package br.com.lett.crawlernode.exceptions;

import java.util.Arrays;

public class RequestException extends RuntimeException {

   private static final String ERROR_MSG = "Missing fields: %s";

   public RequestException(String ...fields) {
      super(String.format(ERROR_MSG, Arrays.toString(fields)));
   }

   public RequestException(Throwable cause, String ...fields) {
      super(String.format(ERROR_MSG, Arrays.toString(fields)), cause);
   }
}
