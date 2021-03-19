package br.com.lett.crawlernode.aws.kinesis;

import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration.ThreadingModel;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.kinesis.producer.Attempt;
import com.amazonaws.services.kinesis.producer.KinesisProducer;
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration;
import com.amazonaws.services.kinesis.producer.UserRecordFailedException;
import com.amazonaws.services.kinesis.producer.UserRecordResult;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.main.GlobalConfigurations;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

public class KPLProducer {

   private static final Logger LOGGER = LoggerFactory.getLogger(KPLProducer.class);

   private static final Random RANDOM = new Random();

   private static final AtomicLong eventsCreated = new AtomicLong(0);
   private static final AtomicLong eventsPut = new AtomicLong(0);

   private static final char RECORD_SEPARATOR = '\n';

   private final KinesisProducer kinesisProducer;

   private final ExecutorService callbackThreadPool = Executors.newCachedThreadPool();

   private static final KPLProducer INSTANCE = new KPLProducer();

   private KPLProducer() {
      KinesisProducerConfiguration config = new KinesisProducerConfiguration()
         .setRegion(KPLProducerConfig.REGION)
         .setCredentialsProvider(new DefaultAWSCredentialsProviderChain())
         .setMaxConnections(KPLProducerConfig.KPL_MAX_CONNECTIONS)
         .setRequestTimeout(KPLProducerConfig.KPL_REQUEST_TIMEOUT)
         .setRecordTtl(KPLProducerConfig.RECORD_TTL) // 5 minutes to avoid data loss
         .setRecordMaxBufferedTime(KPLProducerConfig.RECORD_MAX_BUFFERED_TIME) // Consumer needs to know how to disassemble records
         .setAggregationEnabled(true)
         .setThreadPoolSize(KPLProducerConfig.THREAD_POOL_NUMBER)
         .setThreadingModel(ThreadingModel.POOLED)
         .setMetricsLevel(KPLProducerConfig.METRIC_LEVEL_NONE);

      kinesisProducer = new KinesisProducer(config);
   }

   public static KPLProducer getInstance() {
      return INSTANCE;
   }

   public void close() {
      LOGGER.debug("Stopping KPL ...");

      LOGGER.debug("Running KPL flushSync ...");
      kinesisProducer.flushSync();

      LOGGER.debug("Closing KPL child process ...");
      kinesisProducer.destroy();
   }

   /**
    * Asynchronously put an event to the kinesis internal queue
    *
    * @param r             data to send to kineses
    * @param session       session
    * @param kinesisStream stream name
    */
   public void put(RatingsReviews r, Session session, String kinesisStream) {
      try {
         long countCreated = eventsCreated.incrementAndGet();

         Logging.printLogDebug(LOGGER, session, "Received event " + countCreated);

         ByteBuffer data = ByteBuffer.wrap((r.serializeToKinesis() + RECORD_SEPARATOR).getBytes(StandardCharsets.UTF_8));

         FutureCallback<UserRecordResult> myCallback = getCallback(session);

         ListenableFuture<UserRecordResult> f = kinesisProducer.addUserRecord(kinesisStream,
            randomPartitionKey(), randomExplicitHashKey(), data);

         Futures.addCallback(f, myCallback, callbackThreadPool);


      } catch (Exception e) {
         Logging.printLogError(LOGGER, session, CommonMethods.getStackTrace(e));
      }
   }

   /**
    * Asynchronously put an event to the kinesis internal queue
    *
    * @param p       product to send
    * @param session session
    */
   public void put(Product p, Session session) {
      try {
         long countCreated = eventsCreated.incrementAndGet();

         Logging.printLogDebug(LOGGER, session, "Received event " + countCreated);

         ByteBuffer data = ByteBuffer.wrap((p.serializeToKinesis() + RECORD_SEPARATOR).getBytes(StandardCharsets.UTF_8));

         FutureCallback<UserRecordResult> myCallback = getCallback(session);

         ListenableFuture<UserRecordResult> f = kinesisProducer.addUserRecord(GlobalConfigurations.executionParameters.getKinesisStream(),
            randomPartitionKey(), randomExplicitHashKey(), data);

         Futures.addCallback(f, myCallback, callbackThreadPool);

      } catch (Exception e) {
         Logging.printLogError(LOGGER, session, CommonMethods.getStackTrace(e));
      }
   }

   @NotNull
   private FutureCallback<UserRecordResult> getCallback(Session session) {
      return new FutureCallback<UserRecordResult>() {

         @Override
         public void onFailure(@NotNull Throwable t) {
            if (t instanceof UserRecordFailedException) {
               UserRecordFailedException ex = (UserRecordFailedException) t;
               UserRecordResult r = ex.getResult();
               Attempt last = Iterables.getLast(r.getAttempts());
               Logging.printLogError(LOGGER, session, String.format("Record failed to put - %s(Duration) : %s(ErrorCode) : %s(ErrorMessage)",
                  last.getDuration(), last.getErrorCode(), last.getErrorMessage()));
            }
            Logging.printLogError(LOGGER, session, "Exception during put.");
            Logging.printLogError(LOGGER, session, CommonMethods.getStackTrace(t));
         }

         @Override
         public void onSuccess(UserRecordResult result) {
            Logging.printLogDebug(LOGGER, session, "Succesfully put record: " + result.getSequenceNumber());
            long putCount = eventsPut.incrementAndGet();
            Logging.printLogDebug(LOGGER, session, String.format("Events successfully put so far: %s", putCount));
         }
      };
   }

   /**
    * @return A randomly generated explicit hash key.
    */
   private String randomExplicitHashKey() {
      return new BigInteger(128, RANDOM).toString(10);
   }

   private String randomPartitionKey() {
      return UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
   }

}
