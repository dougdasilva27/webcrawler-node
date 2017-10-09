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

	private static final String SEED_QUEUE_URL 							= "https://sqs.us-east-1.amazonaws.com/127229910321/crawler-seed";
	private static final String INSIGHTS_DEVELOPMENT_QUEUE_URL			= "https://sqs.us-east-1.amazonaws.com/127229910321/crawler-insights-development";
	private static final String DISCOVERY_QUEUE_URL 					= "https://sqs.us-east-1.amazonaws.com/127229910321/crawler-discover";
	private static final String DISCOVERY_WEBDRIVER_QUEUE_URL 			= "https://sqs.us-east-1.amazonaws.com/127229910321/crawler-discover-webdriver";
	private static final String INSIGHTS_QUEUE_URL 						= "https://sqs.us-east-1.amazonaws.com/127229910321/crawler-insights";
	private static final String WEBDRIVER_QUEUE_URL						= "https://sqs.us-east-1.amazonaws.com/127229910321/crawler-insights-webdriver";
	private static final String IMAGES_QUEUE_URL						= "https://sqs.us-east-1.amazonaws.com/127229910321/crawler-images";
	private static final String RATING_QUEUE_URL						= "https://sqs.us-east-1.amazonaws.com/127229910321/crawler-rating";
	private static final String RATING_QUEUE_WEBDRIVER_URL				= "https://sqs.us-east-1.amazonaws.com/127229910321/crawler-rating-webdriver";
	private static final String LAMBDA_URL								= "https://sqs.us-east-1.amazonaws.com/127229910321/lambda-test";
	private static final String RANKING_KEYWORDS_URL					= "https://sqs.us-east-1.amazonaws.com/127229910321/crawler-ranking-keywords";
	private static final String DISCOVER_KEYWORDS_QUEUE_URL				= "https://sqs.us-east-1.amazonaws.com/127229910321/crawler-discover-keywords";
	private static final String RANKING_KEYWORDS_WEBDRIVER_URL			= "https://sqs.us-east-1.amazonaws.com/127229910321/crawler-ranking-keywords-webdriver";
	private static final String DISCOVER_KEYWORDS_WEBDRIVER_QUEUE_URL 	= "https://sqs.us-east-1.amazonaws.com/127229910321/crawler-discover-keywords-webdriver";
	private static final String RANKING_CATEGORIES_URL					= "https://sqs.us-east-1.amazonaws.com/127229910321/crawler-ranking-categories";
	private static final String DISCOVER_CATEGORIES_QUEUE_URL			= "https://sqs.us-east-1.amazonaws.com/127229910321/crawler-discover-categories";
	private static final String INTEREST_PROCESSED_URL 					= "https://sqs.us-east-1.amazonaws.com/127229910321/interest-processed"; 
	

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
		queueURLMap.put(QueueName.DISCOVER_WEBDRIVER, DISCOVERY_WEBDRIVER_QUEUE_URL);
		queueURLMap.put(QueueName.DISCOVER_KEYWORDS_WEBDRIVER, DISCOVER_KEYWORDS_WEBDRIVER_QUEUE_URL);
		queueURLMap.put(QueueName.IMAGES, IMAGES_QUEUE_URL);
		queueURLMap.put(QueueName.INSIGHTS, INSIGHTS_QUEUE_URL);
		queueURLMap.put(QueueName.INSIGHTS_DEVELOPMENT, INSIGHTS_DEVELOPMENT_QUEUE_URL);
		queueURLMap.put(QueueName.RATING, RATING_QUEUE_URL);
		queueURLMap.put(QueueName.SEED, SEED_QUEUE_URL);
		queueURLMap.put(QueueName.RANKING_KEYWORDS, RANKING_KEYWORDS_URL);
		queueURLMap.put(QueueName.DISCOVER_KEYWORDS, DISCOVER_KEYWORDS_QUEUE_URL);
		queueURLMap.put(QueueName.INTEREST_PROCESSED, INTEREST_PROCESSED_URL);
		
		queueURLMap.put(QueueName.RATING_WEBDRIVER, RATING_QUEUE_WEBDRIVER_URL);
		queueURLMap.put(QueueName.LAMBDA, LAMBDA_URL);
		queueURLMap.put(QueueName.RANKING_KEYWORDS_WEBDRIVER, RANKING_KEYWORDS_WEBDRIVER_URL);
		queueURLMap.put(QueueName.RANKING_CATEGORIES, RANKING_CATEGORIES_URL);
		queueURLMap.put(QueueName.DISCOVER_KEYWORDS_WEBDRIVER, DISCOVER_KEYWORDS_WEBDRIVER_QUEUE_URL);
		queueURLMap.put(QueueName.DISCOVER_CATEGORIES, DISCOVER_CATEGORIES_QUEUE_URL);
		queueURLMap.put(QueueName.WEBDRIVER, WEBDRIVER_QUEUE_URL);
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
