package br.com.lett.crawlernode.queue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.sqs.AmazonSQS;

import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageBatchResult;

import br.com.lett.crawlernode.main.ExecutionParameters;
import br.com.lett.crawlernode.main.Main;
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

	private static final String SEED_QUEUE_URL 							= "https://sqs.us-east-1.amazonaws.com/792472451317/crawler-seed";
	private static final String SEED_DEAD_QUEUE_URL 					= "https://sqs.us-east-1.amazonaws.com/792472451317/crawler-seed-dead";

	private static final String INSIGHTS_QUEUE_URL 						= "https://sqs.us-east-1.amazonaws.com/792472451317/crawler-insights";
	private static final String INSIGHTS_DEVELOPMENT_QUEUE_URL			= "https://sqs.us-east-1.amazonaws.com/792472451317/crawler-insights-development";
	private static final String INSIGHTS_DEAD_QUEUE_URL 				= "https://sqs.us-east-1.amazonaws.com/792472451317/crawler-insights-dead";

	private static final String DISCOVERY_QUEUE_URL 					= "https://sqs.us-east-1.amazonaws.com/792472451317/crawler-discover";
	private static final String DISCOVERY_DEAD_QUEUE_URL 				= "https://sqs.us-east-1.amazonaws.com/792472451317/crawler-discover-dead";

	private static final String IMAGES_QUEUE_URL						= "https://sqs.us-east-1.amazonaws.com/792472451317/crawler-images";
	private static final String IMAGES_DEVELOPMENT_QUEUE_URL			= "https://sqs.us-east-1.amazonaws.com/792472451317/crawler-images-development";
	private static final String IMAGES_DEAD_QUEUE_URL					= "https://sqs.us-east-1.amazonaws.com/792472451317/crawler-images-dead";

	private static final String RATING_REVIEWS_QUEUE_URL				= "https://sqs.us-east-1.amazonaws.com/792472451317/crawler-rating_reviews";
	private static final String RATING_REVIEWS_DEAD_QUEUE_URL			= "https://sqs.us-east-1.amazonaws.com/792472451317/crawler-rating_reviews-dead";
	private static final String RATING_REVIEWS_DEVELOPMENT_QUEUE_URL 	= "https://sqs.us-east-1.amazonaws.com/792472451317/crawler-rating_reviews-development";

	private static final String DEVELOMENT_QUEUE_URL 					= "https://sqs.us-east-1.amazonaws.com/792472451317/crawler-development";

	public static final int MAXIMUM_RECEIVE_TIME = 10; // 10 seconds for long pooling
	public static final int MAX_MESSAGES_REQUEST = 10; // the maximum number of messages that Amazon can receive a request for

	public static final String CITY_MESSAGE_ATTR 				= "city";
	public static final String MARKET_MESSAGE_ATTR 				= "market";
	//public static final String MARKET_ID_MESSAGE_ATTR 			= "marketId";
	public static final String PROCESSED_ID_MESSAGE_ATTR 		= "processedId";
	public static final String INTERNAL_ID_MESSAGE_ATTR 		= "internalId";
	public static final String PROXY_SERVICE_MESSAGE_ATTR 		= "proxies";

	public static final String IMAGE_TYPE 						= "type";
	public static final String PRIMARY_IMAGE_TYPE_MESSAGE_ATTR 	= "primary";
	public static final String SECONDARY_IMAGES_MESSAGE_ATTR 	= "secondary";
	public static final String NUMBER_MESSAGE_ATTR				= "number";

	static {
		queueURLMap = new HashMap<String, String>();

		queueURLMap.put(QueueName.DEVELOPMENT, DEVELOMENT_QUEUE_URL);

		queueURLMap.put(QueueName.DISCOVER, DISCOVERY_QUEUE_URL);
		queueURLMap.put(QueueName.DISCOVER_DEAD, DISCOVERY_DEAD_QUEUE_URL);

		queueURLMap.put(QueueName.IMAGES, IMAGES_QUEUE_URL);
		queueURLMap.put(QueueName.IMAGES_DEVELOPMENT, IMAGES_DEVELOPMENT_QUEUE_URL);
		queueURLMap.put(QueueName.IMAGES_DEAD, IMAGES_DEAD_QUEUE_URL);

		queueURLMap.put(QueueName.INSIGHTS, INSIGHTS_QUEUE_URL);
		queueURLMap.put(QueueName.INSIGHTS_DEVELOPMENT, INSIGHTS_DEVELOPMENT_QUEUE_URL);
		queueURLMap.put(QueueName.INSIGHTS_DEAD, INSIGHTS_DEAD_QUEUE_URL);

		queueURLMap.put(QueueName.RATING_REVIEWS_DEVELOPMENT, RATING_REVIEWS_DEVELOPMENT_QUEUE_URL);
		queueURLMap.put(QueueName.RATING_REVIEWS, RATING_REVIEWS_QUEUE_URL);
		queueURLMap.put(QueueName.RATING_REVIEWS_DEAD, RATING_REVIEWS_DEAD_QUEUE_URL);

		queueURLMap.put(QueueName.SEED, SEED_QUEUE_URL);
		queueURLMap.put(QueueName.SEED_DEAD, SEED_DEAD_QUEUE_URL);
	}

	/**
	 * Request for messages (tasks) on the Amazon queue up to a maximum of 10 messages
	 * @return List containing all the messages retrieved
	 */
	private static List<Message> requestMessages(AmazonSQS sqs, String queueName, int maxNumberOfMessages) {
		String queueURL = getQueueURL(queueName);
		ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueURL).withMessageAttributeNames("All");
		receiveMessageRequest.setMaxNumberOfMessages(maxNumberOfMessages);
		List<Message> messages = sqs.receiveMessage(receiveMessageRequest).getMessages();

		return messages;
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
