package br.com.lett.crawlernode.metrics;

import br.com.lett.crawlernode.core.session.Session;
import io.prometheus.client.Gauge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Exporter {
   private static final Gauge GAUGE = Gauge.build()
      .name("error_market").help("Errors by market")
      .labelNames("error_name", "market")
      .register();

   private static final Logger logger = LoggerFactory.getLogger(Exporter.class);

   public static void collectError(Exception e, Session session) {
      logger.debug("Exception collected");
      GAUGE.labels(e.getClass().getSimpleName(), session.getMarket().getName()).inc();
   }
}

