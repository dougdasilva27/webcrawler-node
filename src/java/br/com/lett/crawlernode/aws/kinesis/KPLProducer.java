package br.com.lett.crawlernode.aws.kinesis;

import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.Ranking;
import br.com.lett.crawlernode.core.models.SkuStatus;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.base.Task;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.main.GlobalConfigurations;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.kinesis.producer.*;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KPLProducer {

   private static final Logger LOGGER = LoggerFactory.getLogger(KPLProducer.class);

   private static final Random RANDOM = new Random();

   private static final char RECORD_SEPARATOR = '\n';

   private final KinesisProducer kinesisProducer;

   private final ExecutorService callbackThreadPool = Executors.newCachedThreadPool();

   private static final KPLProducer INSTANCE = new KPLProducer();

   private KPLProducer() {
      KinesisProducerConfiguration config = new KinesisProducerConfiguration();
      config.setRegion(KPLProducerConfig.REGION);
      config.setCredentialsProvider(new DefaultAWSCredentialsProviderChain());
      config.setMaxConnections(KPLProducerConfig.KPL_MAX_CONNECTIONS);
      config.setRequestTimeout(KPLProducerConfig.KPL_REQUEST_TIMEOUT);
      config.setRecordTtl(KPLProducerConfig.RECORD_TTL); // 5 minutes to avoid data loss
      config.setRecordMaxBufferedTime(KPLProducerConfig.RECORD_MAX_BUFFERED_TIME); // Consumer needs to know how to disassemble records
      config.setAggregationEnabled(true);
      config.setMetricsLevel(KPLProducerConfig.METRIC_LEVEL_NONE); // Do not send any KPL metrics do CloudWatch

      kinesisProducer = new KinesisProducer(config);
   }

   public static KPLProducer getInstance() {
      return INSTANCE;
   }

   public void close() {
      LOGGER.debug("Running KPL flushSync ...");
      kinesisProducer.flushSync();

      LOGGER.debug("Closing KPL child process ...");
      kinesisProducer.destroy();
   }

   /**
    * Asynchronously put an event to the kinesis internal queue
    *
    * @param p       product to send
    * @param session session
    */
   public void put(Product p, Session session) {
      ByteBuffer data = ByteBuffer.wrap((p.serializeToKinesis(session) + RECORD_SEPARATOR).getBytes(StandardCharsets.UTF_8));

      FutureCallback<UserRecordResult> myCallback = getCallback(session);

      ListenableFuture<UserRecordResult> f = kinesisProducer.addUserRecord(GlobalConfigurations.executionParameters.getKinesisStream(),
         p.getTimestamp(), randomExplicitHashKey(), data);

      Futures.addCallback(f, myCallback, callbackThreadPool);
   }

   public static void sendMessageCatalogToKinesis(Product crawledProduct, Session session) {

      long productStartTime = System.currentTimeMillis();

      SkuStatus skuStatus = CommonMethods.getSkuStatus(crawledProduct);
      String internalId = crawledProduct.getInternalId();
      Message message = Message.build(skuStatus, session.getSessionId(), internalId, session.getMarket().getId(), session.getSupplierId());

      getInstance().put(message, session);

      JSONObject kinesisProductFlowMetadata = new JSONObject().put("aws_elapsed_time", System.currentTimeMillis() - productStartTime)
         .put("aws_type", "kinesis")
         .put("kinesis_flow_type", "product");

      Logging.logInfo(LOGGER, session, kinesisProductFlowMetadata, "AWS TIMING INFO");

   }

   /**
    * Asynchronously put an event to the kinesis internal queue
    *
    * @param m       message to send
    * @param session session
    */
   public void put(Message m, Session session) {
      ByteBuffer data = ByteBuffer.wrap((m.serializeToKinesis() + RECORD_SEPARATOR).getBytes(StandardCharsets.UTF_8));

      FutureCallback<UserRecordResult> myCallback = getCallback(session);

      ListenableFuture<UserRecordResult> f = kinesisProducer.addUserRecord(GlobalConfigurations.executionParameters.getKinesisStreamCatalog(),
         m.getTimestamp(), randomExplicitHashKey(), data);

      Futures.addCallback(f, myCallback, callbackThreadPool);
   }

   public void put(Ranking ranking, Session session, boolean isRediscovery) {
      RankingModel rankingModel = new RankingModel(ranking);

      ByteBuffer data = ByteBuffer.wrap((rankingModel.serializeToKinesis(session) + RECORD_SEPARATOR).getBytes(StandardCharsets.UTF_8));

      FutureCallback<UserRecordResult> myCallback = getCallback(session);

      String kinesisStream = isRediscovery ? GlobalConfigurations.executionParameters.getKinesisStreamRediscovery() : GlobalConfigurations.executionParameters.getKinesisStreamRanking();

      ListenableFuture<UserRecordResult> f = kinesisProducer.addUserRecord(kinesisStream, rankingModel.getTimestamp().toString(), randomExplicitHashKey(), data);

      Futures.addCallback(f, myCallback, callbackThreadPool);
   }


   private static FutureCallback<UserRecordResult> getCallback(Session session) {
      return new FutureCallback<>() {

         @Override
         public void onFailure(Throwable t) {
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
            Logging.printLogDebug(LOGGER, session, "Successfully put record: " + result.getSequenceNumber());
         }
      };
   }

   /**
    * @return A randomly generated explicit hash key.
    */
   private String randomExplicitHashKey() {
      return new BigInteger(128, RANDOM).toString(10);
   }
}
