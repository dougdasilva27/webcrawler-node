package br.com.lett.crawlernode.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.sqs.AmazonSQS;

import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageRequest;

import br.com.lett.crawlernode.main.ExecutionParameters;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.util.Logging;

/**
 * A bridge with the Amazon SQS. The other crawlers modules uses the methods from this class
 * to send messages, do requests and delete messages from the queue.
 * The long pooling time is sort of in sync with the Timer thread on the main method in class Main.
 * This time is the time that a request on the queue waits until some message is on the request response.
 * @author Samir Leao
 *
 */
public class QueueService {

	protected static final Logger logger = LoggerFactory.getLogger(QueueService.class);
	
	private static final String SEED_QUEUE_URL 					= "https://sqs.us-east-1.amazonaws.com/792472451317/crawler-seed";
	private static final String SEED_DEAD_LETTER_QUEUE_URL 		= "https://sqs.us-east-1.amazonaws.com/792472451317/crawler-seed-dead";

	private static final String INSIGHTS_QUEUE_URL 				= "https://sqs.us-east-1.amazonaws.com/792472451317/crawler-insights";
	private static final String INSIGHTS_DEAD_LETTER_QUEUE_URL 	= "https://sqs.us-east-1.amazonaws.com/792472451317/crawler-insights-dead";

	private static final String DISCOVERY_QUEUE_URL 			= "https://sqs.us-east-1.amazonaws.com/792472451317/crawler-discover";
	private static final String DISCOVERY_DEAD_LETTER_QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/792472451317/crawler-discover-dead";
	
	private static final String IMAGES_QUEUE_URL				= "https://sqs.us-east-1.amazonaws.com/792472451317/crawler-images";
	private static final String IMAGES_DEAD_LETTER_QUEUE_URL	= "https://sqs.us-east-1.amazonaws.com/792472451317/crawler-images-dead";

	private static final String DEVELOMENT_QUEUE_URL 			= "https://sqs.us-east-1.amazonaws.com/792472451317/crawler-development";

	public static final int MAXIMUM_RECEIVE_TIME = 10; // 10 seconds for long pooling
	public static final int MAX_MESSAGES_REQUEST = 10; // the maximum number of messages that Amazon can receive a request for

	public static final String CITY_MESSAGE_ATTR 				= "city";
	public static final String MARKET_MESSAGE_ATTR 				= "market";
	public static final String MARKET_ID_MESSAGE_ATTR 			= "marketId";
	public static final String PROCESSED_ID_MESSAGE_ATTR 		= "processedId";
	public static final String INTERNAL_ID_MESSAGE_ATTR 		= "internalId";
	public static final String PROXY_SERVICE_MESSAGE_ATTR 		= "proxies";
	public static final String SECONDARY_IMAGES_MESSAGE_ATTR 	= "secondary";
	public static final String NUMBER_MESSAGE_ATTR				= "number";
	
	/**
	 * 
	 * @param queueHandler
	 * @param maxNumberOfMessages
	 * @return
	 */
	public static SQSRequestResult requestMessages(QueueHandler queueHandler, int maxNumberOfMessages) {
		SQSRequestResult result = new SQSRequestResult();
		List<Message> messages = null;
		
		if (Main.executionParameters.getEnvironment().equals(ExecutionParameters.ENVIRONMENT_DEVELOPMENT)) {
			Logging.printLogDebug(logger, "Requesting messages from " + QueueHandler.DEVELOPMENT + "...");
			messages = requestMessages(queueHandler.getQueue(QueueHandler.DEVELOPMENT), QueueHandler.DEVELOPMENT, maxNumberOfMessages);
			result.setMessages(messages);
			result.setQueueName(QueueHandler.DEVELOPMENT);
			return result;
		}
		
		messages = requestMessages(queueHandler.getQueue(QueueHandler.SEED), QueueHandler.SEED, maxNumberOfMessages);
		if (!messages.isEmpty()) {
			result.setMessages(messages);
			result.setQueueName(QueueHandler.SEED);
			return result;
		}
		
		messages = requestMessages(queueHandler.getQueue(QueueHandler.INSIGHTS), QueueHandler.INSIGHTS, maxNumberOfMessages);
		if (!messages.isEmpty()) {
			result.setMessages(messages);
			result.setQueueName(QueueHandler.INSIGHTS);
			return result;
		}
		
		if (Main.executionParameters.isImageTaskActivated()) {
			messages = requestMessages(queueHandler.getQueue(QueueHandler.IMAGES), QueueHandler.IMAGES, maxNumberOfMessages);
			if (!messages.isEmpty()) {
				result.setMessages(messages);
				result.setQueueName(QueueHandler.IMAGES);
				return result;
			}
		}
		
		messages = requestMessages(queueHandler.getQueue(QueueHandler.DISCOVER), QueueHandler.DISCOVER, maxNumberOfMessages);
		if (!messages.isEmpty()) {
			result.setMessages(messages);
			result.setQueueName(QueueHandler.DISCOVER);
			return result;
		}
		
		// if all the queues are empty, will return an empty list of messages
		if (result.getMessages() == null) {
			result.setMessages(new ArrayList<Message>());
		}
		
		return result;
	}

	/**
	 * 
	 * @param queueHandler
	 * @param queueName
	 * @param message
	 */
	public static void deleteMessage(QueueHandler queueHandler, String queueName, Message message) {
		AmazonSQS sqs = queueHandler.getQueue(queueName);
		deleteMessage(sqs, queueName, message);
	}

	/**
	 * 
	 * @param queueHandler
	 * @param queueName
	 * @param messageReceiptHandle
	 */
	public static void deleteMessage(QueueHandler queueHandler, String queueName, String messageReceiptHandle) {
		AmazonSQS sqs = queueHandler.getQueue(queueName);
		deleteMessage(sqs, queueName, messageReceiptHandle);
	}

	/**
	 * Request for messages (tasks) on the Amazon queue up to a maximum of 10 messages
	 * @return List containing all the messages retrieved
	 */
	private static List<Message> requestMessages(AmazonSQS sqs, String queueName, int maxNumberOfMessages) {
		String queueURL = selectQueueURL(queueName);
		ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueURL).withMessageAttributeNames("All");
		receiveMessageRequest.setMaxNumberOfMessages(maxNumberOfMessages);
		List<Message> messages = sqs.receiveMessage(receiveMessageRequest).getMessages();

		return messages;
	}

	/**
	 * Delete a message from the sqs
	 * @param sqs
	 * @param message
	 */
	private static void deleteMessage(AmazonSQS sqs, String queueName, Message message) {		
		String queueURL = selectQueueURL(queueName);
		String messageReceiptHandle = message.getReceiptHandle();
		sqs.deleteMessage(new DeleteMessageRequest(queueURL, messageReceiptHandle));
	}

	/**
	 * Delete a message from the sqs
	 * @param sqs
	 * @param messageId
	 * @param messageReceiptHandle
	 */
	private static void deleteMessage(AmazonSQS sqs, String queueName, String messageReceiptHandle) {
		String queueURL = selectQueueURL(queueName);
		sqs.deleteMessage(new DeleteMessageRequest(queueURL, messageReceiptHandle));
	}

	/**
	 * Check a message for the mandatory fields.
	 * @param message
	 * @return true if all fields are ok or false if there is at least one field missing
	 */
	public static boolean checkMessageIntegrity(Message message, String queueName) {
		Map<String, MessageAttributeValue> attrMap = message.getMessageAttributes();

		if (!attrMap.containsKey(QueueService.MARKET_MESSAGE_ATTR)) {
			Logging.printLogError(logger, "Message is missing field [" + MARKET_MESSAGE_ATTR + "]");
			return false;
		}
		if (!attrMap.containsKey(QueueService.CITY_MESSAGE_ATTR)) {
			Logging.printLogError(logger, "Message is missing field [" + CITY_MESSAGE_ATTR + "]");
			return false;
		}

		// specific fields according with queue type
		if (queueName.equals(QueueHandler.INSIGHTS)) {
			if (Main.executionParameters.getEnvironment().equals(ExecutionParameters.ENVIRONMENT_PRODUCTION)) {
				if (!attrMap.containsKey(QueueService.PROCESSED_ID_MESSAGE_ATTR)) {
					Logging.printLogError(logger, "Message is missing field [" + PROCESSED_ID_MESSAGE_ATTR + "]");
					return false;
				}
				if (!attrMap.containsKey(QueueService.INTERNAL_ID_MESSAGE_ATTR)) {
					Logging.printLogError(logger, "Message is missing field [" + INTERNAL_ID_MESSAGE_ATTR + "]");
					return false;
				}
			}
		}
		
		// message from the images queue must have the secondary field in it's attributes
		if (queueName.equals(QueueHandler.IMAGES)) {
			if (!attrMap.containsKey(QueueService.SECONDARY_IMAGES_MESSAGE_ATTR)) {
				Logging.printLogError(logger, "Message is missing field [" + SECONDARY_IMAGES_MESSAGE_ATTR + "]");
				return false;
			}
		}

		return true;
	}

	/**
	 * Selects a proper Amazon SQS queue to be used, according to it's name.
	 * 
	 * @param queueName the name of the queue, as displayed in Amazon console
	 * @return The appropriate queue URL
	 */
	private static String selectQueueURL(String queueName) {
		if (queueName.equals(QueueHandler.SEED)) return SEED_QUEUE_URL;
		if (queueName.equals(QueueHandler.SEED_DEAD)) return SEED_DEAD_LETTER_QUEUE_URL;
		
		if (queueName.equals(QueueHandler.INSIGHTS)) return INSIGHTS_QUEUE_URL;
		if (queueName.equals(QueueHandler.INSIGHTS_DEAD)) return INSIGHTS_DEAD_LETTER_QUEUE_URL;
		
		if (queueName.equals(QueueHandler.IMAGES)) return IMAGES_QUEUE_URL;
		if (queueName.equals(QueueHandler.IMAGES_DEAD)) return IMAGES_DEAD_LETTER_QUEUE_URL;
		
		if (queueName.equals(QueueHandler.DISCOVER)) return DISCOVERY_QUEUE_URL;
		if (queueName.equals(QueueHandler.DISCOVER_DEAD)) return DISCOVERY_DEAD_LETTER_QUEUE_URL;
		
		if (queueName.equals(QueueHandler.DEVELOPMENT)) return DEVELOMENT_QUEUE_URL;

		Logging.printLogError(logger, "Unrecognized queue.");
		
		return null;
	}

}
