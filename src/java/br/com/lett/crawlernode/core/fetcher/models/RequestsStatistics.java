package br.com.lett.crawlernode.core.fetcher.models;

public class RequestsStatistics {

   private int statusCode;
   private int attempt;
   private boolean hasPassedValidation;
   private LettProxy proxy;
   private long elapsedTime = 0;

   public int getStatusCode() {
      return statusCode;
   }

   public void setStatusCode(int statusCode) {
      this.statusCode = statusCode;
   }

   public int getAttempt() {
      return attempt;
   }

   public void setAttempt(int attempt) {
      this.attempt = attempt;
   }

   public boolean isHasPassedValidation() {
      return hasPassedValidation;
   }

   public void setHasPassedValidation(boolean hasPassedValidation) {
      this.hasPassedValidation = hasPassedValidation;
   }

   public LettProxy getProxy() {
      return proxy;
   }

   public void setProxy(LettProxy proxy) {
      this.proxy = proxy;
   }

   public long getElapsedTime() {
      return elapsedTime;
   }

   public void setElapsedTime(long elapsedTime) {
      this.elapsedTime = elapsedTime;
   }
}
