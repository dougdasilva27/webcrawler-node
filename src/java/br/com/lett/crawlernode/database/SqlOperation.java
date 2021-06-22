package br.com.lett.crawlernode.database;

import java.util.Locale;

public enum SqlOperation {
   SELECT, UPDATE, DELETE;

   @Override
   public String toString() {
      return name().toLowerCase(Locale.ROOT);
   }
}
