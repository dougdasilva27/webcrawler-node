package br.com.lett.crawlernode.integration.redis.config;

public enum RedisDb {

   CRAWLER(1), RANKING(2), CATALOG(3);

   public final Integer number;

   RedisDb(Integer number) {
      this.number = number;
   }
}
