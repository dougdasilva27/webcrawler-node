package br.com.lett.crawlernode.aws.sqs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageBatchResult;
import br.com.lett.crawlernode.main.GlobalConfigurations;
import br.com.lett.crawlernode.util.Logging;
import enums.QueueName;

/**
 * A bridge with the Amazon SQS. The other crawlers modules uses the methods from this class to send
 * messages, do requests and delete messages from the queue. The long pooling time is sort of in
 * sync with the Timer thread on the main method in class Main. This time is the time that a request
 * on the queue waits until some message is on the request response.
 * 
 * @author Samir Leao
 *
 */
public class QueueService {

  protected static final Logger logger = LoggerFactory.getLogger(QueueService.class);

  private static final Map<String, String> queueURLMap;


  public static final String MARKET_ID_MESSAGE_ATTR = "marketId";
  public static final String PROCESSED_ID_MESSAGE_ATTR = "processedId";
  public static final String INTERNAL_ID_MESSAGE_ATTR = "internalId";

  public static final String IMAGE_TYPE = "type";
  public static final String PRIMARY_IMAGE_TYPE_MESSAGE_ATTR = "primary";
  public static final String SECONDARY_IMAGES_MESSAGE_ATTR = "secondary";
  public static final String NUMBER_MESSAGE_ATTR = "number";

  private static final String QUEUE_URL = GlobalConfigurations.executionParameters.getQueueUrlFirstPart();

  static {
    queueURLMap = new HashMap<>();
    queueURLMap.put(QueueName.CORE.name(), QUEUE_URL + QueueName.CORE.name());
    queueURLMap.put(QueueName.CORE_WEBDRIVER.name(), QUEUE_URL + QueueName.CORE_WEBDRIVER.name());
    queueURLMap.put(QueueName.DISCOVERER.name(), QUEUE_URL + QueueName.DISCOVERER.name());
    queueURLMap.put(QueueName.DISCOVERER_WEBDRIVER.name(), QUEUE_URL + QueueName.DISCOVERER_WEBDRIVER.name());
    queueURLMap.put(QueueName.DISCOVERER_BY_KEYWORDS.name(), QUEUE_URL + QueueName.DISCOVERER_BY_KEYWORDS.name());
    queueURLMap.put(QueueName.DISCOVERER_BY_KEYWORDS_WEBDRIVER.name(), QUEUE_URL + QueueName.DISCOVERER_BY_KEYWORDS_WEBDRIVER.name());
    queueURLMap.put(QueueName.DISCOVERER_BY_CATEGORIES.name(), QUEUE_URL + QueueName.DISCOVERER_BY_CATEGORIES.name());
    queueURLMap.put(QueueName.IMAGES_DOWNLOAD.name(), QUEUE_URL + QueueName.IMAGES_DOWNLOAD.name());
    queueURLMap.put(QueueName.RANKING_BY_KEYWORDS.name(), QUEUE_URL + QueueName.RANKING_BY_KEYWORDS.name());
    queueURLMap.put(QueueName.RANKING_BY_KEYWORDS_WEBDRIVER.name(), QUEUE_URL + QueueName.RANKING_BY_KEYWORDS_WEBDRIVER.name());
    queueURLMap.put(QueueName.RANKING_BY_CATEGORIES.name(), QUEUE_URL + QueueName.RANKING_BY_CATEGORIES.name());
    queueURLMap.put(QueueName.RATING.name(), QUEUE_URL + QueueName.RATING.name());
    queueURLMap.put(QueueName.RATING_WEBDRIVER.name(), QUEUE_URL + QueueName.RATING_WEBDRIVER.name());
    queueURLMap.put(QueueName.SEED.name(), QUEUE_URL + QueueName.SEED.name());

    queueURLMap.put(QueueName.INTEREST_PROCESSED.name(), QUEUE_URL + QueueName.INTEREST_PROCESSED.name());
    queueURLMap.put(QueueName.INTEREST_PROCESSED_RATING.name(), QUEUE_URL + QueueName.INTEREST_PROCESSED_RATING.name());
  }

  /**
   * Send a message batch to SQS.
   * 
   * @param sqs
   * @param entries
   * @return
   */
  public static SendMessageBatchResult sendBatchMessages(AmazonSQS sqs, String queueName, List<SendMessageBatchRequestEntry> entries) {
    SendMessageBatchRequest batchMessageBatchRequest = new SendMessageBatchRequest();
    String queueURL = getQueueURL(queueName);
    batchMessageBatchRequest.setQueueUrl(queueURL);
    batchMessageBatchRequest.setEntries(entries);

    return sqs.sendMessageBatch(batchMessageBatchRequest);
  }

  /**
   * Selects a proper Amazon SQS queue to be used, according to it's name.
   * 
   * @param queueName the name of the queue, as displayed in Amazon console
   * @return The appropriate queue URL
   */
  private static String getQueueURL(String queueName) {
    if (queueURLMap.containsKey(queueName)) {
      return queueURLMap.get(queueName);
    }

    Logging.printLogError(logger, "Unrecognized queue.");
    return null;
  }

}
