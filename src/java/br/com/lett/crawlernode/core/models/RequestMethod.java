package br.com.lett.crawlernode.core.models;

public enum RequestMethod {
   POST, GET, DELETE, PUT, PATCH;

   @Override
   public String toString() {
      return name();
   }
}
