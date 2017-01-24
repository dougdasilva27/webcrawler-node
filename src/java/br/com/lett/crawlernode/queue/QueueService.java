package br.com.lett.crawlernode.queue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.sqs.AmazonSQS;

import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageBatchResult;

import br.com.lett.crawlernode.util.Logging;

/**
 * A bridge with the Amazon SQS. The other crawlers modules uses the methods from this class
 * to send messages, do requests and delete messages from the queue.
 * The long pooling time is sort of in sync with the Timer thread on the main method in class Main.
 * This time is the time that a request on the queue waits until some message is on the request response.
 * 
 * @author Samir Leao
 *
 */
public class QueueService {

	protected static final Logger logger = LoggerFactory.getLogger(QueueService.class);

	private static final Map<String, String> queueURLMap;

	private static final String SEED_QUEUE_URL 							= "https://sqs.us-east-1.amazonaws.com/354284365376/crawler-seed";

	private static final String INSIGHTS_QUEUE_URL 						= "https://sqs.us-east-1.amazonaws.com/354284365376/crawler-insights-new";
	private static final String INSIGHTS_DEVELOPMENT_QUEUE_URL			= "https://sqs.us-east-1.amazonaws.com/354284365376/crawler-insights-development";

	private static final String DISCOVERY_QUEUE_URL 					= "https://sqs.us-east-1.amazonaws.com/354284365376/crawler-discover";

	private static final String IMAGES_QUEUE_URL						= "https://sqs.us-east-1.amazonaws.com/354284365376/crawler-images";

	private static final String RATING_QUEUE_URL						= "https://sqs.us-east-1.amazonaws.com/354284365376/crawler-rating";

	public static final String MARKET_ID_MESSAGE_ATTR 			= "marketId";
	public static final String PROCESSED_ID_MESSAGE_ATTR 		= "processedId";
	public static final String INTERNAL_ID_MESSAGE_ATTR 		= "internalId";

	public static final String IMAGE_TYPE 						= "type";
	public static final String PRIMARY_IMAGE_TYPE_MESSAGE_ATTR 	= "primary";
	public static final String SECONDARY_IMAGES_MESSAGE_ATTR 	= "secondary";
	public static final String NUMBER_MESSAGE_ATTR				= "number";

	static {
		queueURLMap = new HashMap<>();

		queueURLMap.put(QueueName.DISCOVER, DISCOVERY_QUEUE_URL);

		queueURLMap.put(QueueName.IMAGES, IMAGES_QUEUE_URL);

		queueURLMap.put(QueueName.INSIGHTS, INSIGHTS_QUEUE_URL);
		queueURLMap.put(QueueName.INSIGHTS_DEVELOPMENT, INSIGHTS_DEVELOPMENT_QUEUE_URL);

		queueURLMap.put(QueueName.RATING, RATING_QUEUE_URL);

		queueURLMap.put(QueueName.SEED, SEED_QUEUE_URL);
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
