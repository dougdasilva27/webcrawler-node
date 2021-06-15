package br.com.lett.crawlernode.integration.redis.config;

import java.util.Locale;

public enum RedisDb {

   CRAWLER, RANKING, CATALOG;

   @Override
   public String toString() {
      return name().toLowerCase(Locale.ROOT);
   }
}
