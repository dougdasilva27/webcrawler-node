package br.com.lett.crawlernode.aws.kinesis;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
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

  private KinesisProducer kinesisProducer;

  private static final KPLProducer INSTANCE = new KPLProducer();
  
  class CustomAwsCredentialsProvider implements AWSCredentialsProvider {

	@Override
	public AWSCredentials getCredentials() {
		return new BasicAWSCredentials(
				GlobalConfigurations.executionParameters.getNewAwsAccessKey(), 
				GlobalConfigurations.executionParameters.getNewAwsSecretKey());
	}

	@Override
	public void refresh() {
		// no-op
	}
	  
  }

  private KPLProducer() {
    KinesisProducerConfiguration config = new KinesisProducerConfiguration();
    config.setRegion(KPLProducerConfig.REGION);
    config.setCredentialsProvider(new CustomAwsCredentialsProvider());
    config.setMaxConnections(KPLProducerConfig.KPL_MAX_CONNECTIONS);
    config.setRequestTimeout(KPLProducerConfig.KPL_REQUEST_TIMEOUT);
    config.setRecordTtl(KPLProducerConfig.RECORD_TTL); // 5 minutes to avoid data loss
    config.setRecordMaxBufferedTime(KPLProducerConfig.RECORD_MAX_BUFFERED_TIME); // Consumer needs to know how to disassemble records
    config.setAggregationEnabled(true);

    kinesisProducer = new KinesisProducer(config);
  }

  public static KPLProducer getInstance() {
    return INSTANCE;
  }

  public void close() {
    LOGGER.debug("Stoping KPL ...");

    LOGGER.debug("Running KPL flushSync ...");
    kinesisProducer.flushSync();

    LOGGER.debug("Closing KPL child process ...");
    kinesisProducer.destroy();
  }

  /**
   * Assynchronously put an event to the kinesis internal queue
   * 
   * @param r
   */
  public void put(RatingsReviews r, Session session) {
    try {
      long countCreated = eventsCreated.incrementAndGet();

      Logging.printLogDebug(LOGGER, session, "Received event " + countCreated);

      ByteBuffer data = ByteBuffer.wrap(new StringBuilder().append(r.serializeToKinesis()).append(RECORD_SEPARATOR).toString().getBytes("UTF-8"));

      FutureCallback<UserRecordResult> myCallback = new FutureCallback<UserRecordResult>() {

        /* Analyze and respond to the failure */
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
          Logging.printLogDebug(LOGGER, session, "Succesfully put record: " + result.getSequenceNumber());
          long putCount = eventsPut.incrementAndGet();
          Logging.printLogDebug(LOGGER, session, String.format("Events successfully put so far: %s", putCount));
        }
      };

      // TIMESTAMP is our partition key
      ListenableFuture<UserRecordResult> f =
          kinesisProducer.addUserRecord(GlobalConfigurations.executionParameters.getKinesisStream(), r.getTimestamp(), randomExplicitHashKey(), data);

      Futures.addCallback(f, myCallback);

    } catch (Exception e) {
      Logging.printLogError(LOGGER, session, CommonMethods.getStackTrace(e));
    }
  }

  /**
   * Assynchronously put an event to the kinesis internal queue
   * 
   * @param p
   */
  public void put(Product p, Session session) {
    try {
      long countCreated = eventsCreated.incrementAndGet();

      Logging.printLogDebug(LOGGER, session, "Received event " + countCreated);

      ByteBuffer data = ByteBuffer.wrap(new StringBuilder().append(p.serializeToKinesis()).append(RECORD_SEPARATOR).toString().getBytes("UTF-8"));

      FutureCallback<UserRecordResult> myCallback = new FutureCallback<UserRecordResult>() {

        /* Analyze and respond to the failure */
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
          Logging.printLogDebug(LOGGER, session, "Succesfully put record: " + result.getSequenceNumber());
          long putCount = eventsPut.incrementAndGet();
          Logging.printLogDebug(LOGGER, session, String.format("Events successfully put so far: %s", putCount));
        }
      };

      // TIMESTAMP is our partition key
      ListenableFuture<UserRecordResult> f =
          kinesisProducer.addUserRecord(GlobalConfigurations.executionParameters.getKinesisStream(), p.getTimestamp(), randomExplicitHashKey(), data);

      Futures.addCallback(f, myCallback);

    } catch (Exception e) {
      Logging.printLogError(LOGGER, session, CommonMethods.getStackTrace(e));
    }
  }

  /**
   * @return A randomly generated explicit hash key.
   */
  private String randomExplicitHashKey() {
    return new BigInteger(128, RANDOM).toString(10);
  }

}
