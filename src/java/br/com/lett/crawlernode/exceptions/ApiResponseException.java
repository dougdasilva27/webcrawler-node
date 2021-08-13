package br.com.lett.crawlernode.exceptions;

public class ApiResponseException extends Exception {

   private String errorMessage;

   public ApiResponseException(String errorMessage) {
      super("Api response error: " + errorMessage);
   }

   public String getErrorMessage() {
      return errorMessage;
   }

   public void setErrorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
   }
}
