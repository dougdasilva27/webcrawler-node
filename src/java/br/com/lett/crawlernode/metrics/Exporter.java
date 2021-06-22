package br.com.lett.crawlernode.metrics;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.database.SqlOperation;
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
      .name("requests_latency_seconds").help("Request latency in seconds.")
      .labelNames("operation")
      .register();


   public static void collectError(Exception e, Session session) {
      logger.debug("Exception collected");
      ERROR_COUNTER.labels(e.getClass().getSimpleName(), session.getMarket().getName()).inc();
   }

   public static <E> E collectQuery(SqlOperation operation, Callable<E> callable) {
      return POSTGRES_TIMING.labels(operation.toString())
         .time(callable);
   }
}

