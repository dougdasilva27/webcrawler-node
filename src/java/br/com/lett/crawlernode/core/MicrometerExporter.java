package br.com.lett.crawlernode.core;

import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTracker;
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.binder.jvm.*;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

public class MicrometerExporter {

   private final PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

   private static final MicrometerExporter INSTANCE = new MicrometerExporter();

   private MicrometerExporter() {
      new MicrometerMetricsTrackerFactory(registry);
      new ClassLoaderMetrics().bindTo(registry);
      new JvmMemoryMetrics().bindTo(registry);
      new JvmGcMetrics().bindTo(registry);
      new JvmInfoMetrics().bindTo(registry);
      new ProcessorMetrics().bindTo(registry);
      new JvmThreadMetrics().bindTo(registry);

      registry.config().meterFilter(MicrometerExporter.hikariFilter());
   }

   public static MicrometerExporter getInstance() {
      return INSTANCE;
   }

   public PrometheusMeterRegistry getRegistry() {
      return registry;
   }

   public static MeterFilter hikariFilter() {
      return new MeterFilter() {
         @Override
         public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
            if (id.getName().startsWith(MicrometerMetricsTracker.HIKARI_METRIC_NAME_PREFIX)) {
               return DistributionStatisticConfig.builder()
                  .percentiles(0.95, 1)
                  .build()
                  .merge(config);
            }
            return config;
         }
      };
   }
}
