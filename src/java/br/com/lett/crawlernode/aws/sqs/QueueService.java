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

   public static final String QUEUE_DATA_TYPE_STRING = "String";

   public static final String MARKET_ID_MESSAGE_ATTR = "marketId";
   public static final String PROCESSED_ID_MESSAGE_ATTR = "processedId";
   public static final String INTERNAL_ID_MESSAGE_ATTR = "internalId";
   public static final String SCRAPER_TYPE_MESSAGE_ATTR = "scraperType";

   public static final String IMAGE_TYPE = "type";
   public static final String PRIMARY_IMAGE_TYPE_MESSAGE_ATTR = "primary";
   public static final String SECONDARY_IMAGES_MESSAGE_ATTR = "secondary";
   public static final String NUMBER_MESSAGE_ATTR = "number";

   private static final String QUEUE_URL = GlobalConfigurations.executionParameters.getQueueUrlFirstPart();

   static {
      queueURLMap = new HashMap<>();
      queueURLMap.put(QueueName.CORE.toString(), QUEUE_URL + QueueName.CORE.toString());
      queueURLMap.put(QueueName.CORE_EQI.toString(), QUEUE_URL + QueueName.CORE_EQI.toString());
      queueURLMap.put(QueueName.CORE_EQI_WEBDRIVER.toString(), QUEUE_URL + QueueName.CORE_EQI_WEBDRIVER.toString());
      queueURLMap.put(QueueName.CORE_WEBDRIVER.toString(), QUEUE_URL + QueueName.CORE_WEBDRIVER.toString());
      queueURLMap.put(QueueName.DISCOVERER.toString(), QUEUE_URL + QueueName.DISCOVERER.toString());
      queueURLMap.put(QueueName.DISCOVERER_WEBDRIVER.toString(), QUEUE_URL + QueueName.DISCOVERER_WEBDRIVER.toString());
      queueURLMap.put(QueueName.DISCOVERER_BY_KEYWORDS.toString(), QUEUE_URL + QueueName.DISCOVERER_BY_KEYWORDS.toString());
      queueURLMap.put(QueueName.DISCOVERER_BY_KEYWORDS_WEBDRIVER.toString(), QUEUE_URL + QueueName.DISCOVERER_BY_KEYWORDS_WEBDRIVER.toString());
      queueURLMap.put(QueueName.DISCOVERER_BY_CATEGORIES.toString(), QUEUE_URL + QueueName.DISCOVERER_BY_CATEGORIES.toString());
      queueURLMap.put(QueueName.PRODUCT_IMAGE_DOWNLOAD.toString(), QUEUE_URL + QueueName.PRODUCT_IMAGE_DOWNLOAD.toString());
      queueURLMap.put(QueueName.RANKING_BY_KEYWORDS.toString(), QUEUE_URL + QueueName.RANKING_BY_KEYWORDS.toString());
      queueURLMap.put(QueueName.RANKING_BY_KEYWORDS_WEBDRIVER.toString(), QUEUE_URL + QueueName.RANKING_BY_KEYWORDS_WEBDRIVER.toString());
      queueURLMap.put(QueueName.RANKING_BY_CATEGORIES.toString(), QUEUE_URL + QueueName.RANKING_BY_CATEGORIES.toString());
      queueURLMap.put(QueueName.SEED.toString(), QUEUE_URL + QueueName.SEED.toString());

      queueURLMap.put(QueueName.INTEREST_PROCESSED.toString(), QUEUE_URL + QueueName.INTEREST_PROCESSED.toString());
      queueURLMap.put(QueueName.INTEREST_PROCESSED_RATING.toString(), QUEUE_URL + QueueName.INTEREST_PROCESSED_RATING.toString());

      queueURLMap.put(QueueName.CORE_DEV.toString(),QUEUE_URL+QueueName.CORE_DEV.toString());


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
