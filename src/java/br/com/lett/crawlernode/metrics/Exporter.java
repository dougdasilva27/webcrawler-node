package br.com.lett.crawlernode.metrics;

import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.database.model.SqlOperation;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

public class Exporter {

   private static final Logger logger = LoggerFactory.getLogger(Exporter.class);

   private static final Counter ERROR_COUNTER = Counter.build()
      .name("error_market").help("Errors by market")
      .labelNames("error_name", "market")
      .register();

   static final Histogram POSTGRES_TIMING = Histogram.build()
      .name("database_latency").help("Database latency in seconds.")
      .labelNames("operation")
      .register();

   static final Histogram ENDPOINT_TIMING = Histogram.build()
      .name("request_latency").help("Request latency in seconds.")
      .labelNames("market")
      .register();

   public static void collectError(Exception e, Session session) {
      logger.debug("Exception collected");
      ERROR_COUNTER.labels(e.getClass().getSimpleName(), session.getMarket().getName()).inc();
   }

   public static <E> E collectQuery(SqlOperation operation, Callable<E> callable) {
      return POSTGRES_TIMING.labels(operation.toString())
         .time(callable);
   }

   public static void collectEndpoint(String market, Runnable callable) {
      ENDPOINT_TIMING.labels(market)
         .time(callable);
   }
}

