package br.com.lett.crawlernode.queue;

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
import com.amazonaws.services.sqs.model.SendMessageRequest;

import br.com.lett.crawlernode.kernel.ExecutionParameters;
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
	
	private static final String PRODUCTION_QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/792472451317/crawler-insights";
	private static final String DEVELOMENT_QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/792472451317/crawler-development";
	
	public static final int MAXIMUM_RECEIVE_TIME = 10; // 10 seconds for long pooling
	public static final String CITY_MESSAGE_ATTR = "city";
	public static final String MARKET_MESSAGE_ATTR = "market";
	public static final String MARKET_ID_MESSAGE_ATTR = "marketId";
	public static final String PROCESSED_ID_MESSAGE_ATTR = "processedId";
	public static final String INTERNAL_ID_MESSAGE_ATTR = "internalId";
	public static final String PROXY_SERVICE_MESSAGE_ATTR = "proxy";


	/**
	 * Request for messages (tasks) on the Amazon queue
	 * @return List containing all the messages retrieved
	 */
	public static List<Message> requestMessages(AmazonSQS sqs) {
		Logging.printLogDebug(logger, "Requesting for a maximum of 1 task on queue...");
		
		String queueURL = selectQueue();
		ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueURL).withMessageAttributeNames("All");
		List<Message> messages = sqs.receiveMessage(receiveMessageRequest).getMessages();

		Logging.printLogDebug(logger, "Request returned with " + messages.size() + " tasks");

		return messages;
	}
	
	/**
	 * Request for messages (tasks) on the Amazon queue up to a maximum of 10 messages
	 * @return List containing all the messages retrieved
	 */
	public static List<Message> requestMessages(AmazonSQS sqs, int maxNumberOfMessages) {
		Logging.printLogDebug(logger, "Requesting for a maximum of " + maxNumberOfMessages + " tasks on queue...");
		
		String queueURL = selectQueue();
		ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueURL).withMessageAttributeNames("All");
		receiveMessageRequest.setMaxNumberOfMessages(maxNumberOfMessages);
		List<Message> messages = sqs.receiveMessage(receiveMessageRequest).getMessages();

		Logging.printLogDebug(logger, "Request returned with " + messages.size() + " tasks");

		return messages;
	}
	
	/**
	 * Delete a message from the sqs
	 * @param sqs
	 * @param message
	 */
	public static void deleteMessage(AmazonSQS sqs, Message message) {
		Logging.printLogDebug(logger, "Deleting message " + message.getMessageId());
		
		String queueURL = selectQueue();
		String messageReceiptHandle = message.getReceiptHandle();
		sqs.deleteMessage(new DeleteMessageRequest(queueURL, messageReceiptHandle));
	}
	
	/**
	 * Delete a message from the sqs
	 * @param sqs
	 * @param messageId
	 * @param messageReceiptHandle
	 */
	public static void deleteMessage(AmazonSQS sqs, String messageId, String messageReceiptHandle) {
		Logging.printLogDebug(logger, "Deleting message " + messageId);
		String queueURL = selectQueue();
		sqs.deleteMessage(new DeleteMessageRequest(queueURL, messageReceiptHandle));
	}

	/**
	 * Delete a list of messages from the queue
	 * @param sqs
	 * @param messages
	 */
	public static void deleteMessages(AmazonSQS sqs, List<Message> messages) {
		System.out.println("Deleting received messages.\n");
		String queueURL = selectQueue();
		for (int i = 0; i < messages.size(); i++) {
			String messageReceiptHandle = messages.get(i).getReceiptHandle();
			sqs.deleteMessage(new DeleteMessageRequest(queueURL, messageReceiptHandle));
		}
	}
	
	/**
	 * Send a message with the specified attributes and body to the SQS
	 * @param sqs
	 * @param attributes
	 * @param messageBody
	 */
	public static void sendMessage(AmazonSQS sqs, Map<String, MessageAttributeValue> attributes, String messageBody) {
		SendMessageRequest sendMessageRequest = new SendMessageRequest();
		String queueURL = selectQueue();
		sendMessageRequest.setQueueUrl(queueURL);
		sendMessageRequest.setMessageBody(messageBody);
		sendMessageRequest.setMessageAttributes(attributes);
		
		sqs.sendMessage(sendMessageRequest);
	}
	
	/**
	 * Send a message batch to SQS
	 * @param sqs
	 * @param entries
	 */
	public static void sendBatchMessages(AmazonSQS sqs, List<SendMessageBatchRequestEntry> entries) {
		SendMessageBatchRequest batchMessageBatchRequest = new SendMessageBatchRequest();
		String queueURL = selectQueue();
		batchMessageBatchRequest.setQueueUrl(queueURL);
		batchMessageBatchRequest.setEntries(entries);
		
		sqs.sendMessageBatch(batchMessageBatchRequest);
	}

	/**
	 * Check a message for the mandatory fields.
	 * @param message
	 * @return true if all fields are ok or false if there is at least one field missing
	 */
	public static boolean checkMessage(Message message) {
		Map<String, MessageAttributeValue> attrMap = message.getMessageAttributes();

		if (!attrMap.containsKey(QueueService.MARKET_ID_MESSAGE_ATTR)) {
			Logging.printLogError(logger, "Message is missing field [" + MARKET_ID_MESSAGE_ATTR + "]");
			return false;
		}
		if (!attrMap.containsKey(QueueService.MARKET_MESSAGE_ATTR)) {
			Logging.printLogError(logger, "Message is missing field [" + MARKET_MESSAGE_ATTR + "]");
			return false;
		}
		if (!attrMap.containsKey(QueueService.CITY_MESSAGE_ATTR)) {
			Logging.printLogError(logger, "Message is missing field [" + CITY_MESSAGE_ATTR + "]");
			return false;
		}
		
		if (Main.executionParameters.getMode().equals(ExecutionParameters.MODE_INSIGHTS)) {
			if (!attrMap.containsKey(QueueService.PROCESSED_ID_MESSAGE_ATTR)) {
				Logging.printLogError(logger, "Message is missing field [" + PROCESSED_ID_MESSAGE_ATTR + "]");
				return false;
			}
			if (!attrMap.containsKey(QueueService.INTERNAL_ID_MESSAGE_ATTR)) {
				Logging.printLogError(logger, "Message is missing field [" + INTERNAL_ID_MESSAGE_ATTR + "]");
				return false;
			}
		}

		return true;
	}
	
	/**
	 * Selects a proper Amazon SQS queue to be used, according to the environment
	 * @param environment
	 * @return
	 */
	private static String selectQueue() {
		if (Main.executionParameters.getEnvironment().equals(ExecutionParameters.ENVIRONMENT_PRODUCTION)) {
			return PRODUCTION_QUEUE_URL;
		}
		return DEVELOMENT_QUEUE_URL;
	}

}
