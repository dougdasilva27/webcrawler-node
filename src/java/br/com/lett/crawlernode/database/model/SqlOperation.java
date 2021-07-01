package br.com.lett.crawlernode.database.model;

import java.util.Locale;

public enum SqlOperation {
   SELECT, UPDATE;

   @Override
   public String toString() {
      return name().toLowerCase(Locale.ROOT);
   }
}
