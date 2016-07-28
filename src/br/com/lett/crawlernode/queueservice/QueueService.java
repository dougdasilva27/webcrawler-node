package br.com.lett.crawlernode.queueservice;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.sqs.AmazonSQS;

import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;

import br.com.lett.crawlernode.util.Logging;

/**
 * 
 * @author samirleao
 *
 */
public class QueueService {

	protected static final Logger logger = LoggerFactory.getLogger(QueueService.class);
	
	private static final String QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/792472451317/crawler-insights";
	
	public static final int MAXIMUM_RECEIVE_TIME = 10; // 10 seconds for long pooling
	public static final String CITY_MESSAGE_ATTR = "city";
	public static final String MARKET_MESSAGE_ATTR = "market";
	public static final String MARKET_ID_MESSAGE_ATTR = "marketId";
	public static final String PROCESSED_ID_MESSAGE_ATTR = "processedId";


	/**
	 * Request for messages (tasks) on the Amazon queue
	 * @return List containing all the messages retrieved
	 */
	public static List<Message> requestMessages(AmazonSQS sqs) {
		Logging.printLogDebug(logger, "Requesting for a maximum of 1 task on queue...");

		ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(QUEUE_URL).withMessageAttributeNames("All");
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

		ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(QUEUE_URL).withMessageAttributeNames("All");
		receiveMessageRequest.setMaxNumberOfMessages(10);
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

		String messageReceiptHandle = message.getReceiptHandle();
		sqs.deleteMessage(new DeleteMessageRequest(QUEUE_URL, messageReceiptHandle));
	}
	
	/**
	 * Delete a message from the sqs
	 * @param sqs
	 * @param messageId
	 * @param messageReceiptHandle
	 */
	public static void deleteMessage(AmazonSQS sqs, String messageId, String messageReceiptHandle) {
		Logging.printLogDebug(logger, "Deleting message " + messageId);
		sqs.deleteMessage(new DeleteMessageRequest(QUEUE_URL, messageReceiptHandle));
	}

	/**
	 * Delete a list of messages from the queue
	 * @param sqs
	 * @param messages
	 */
	public static void deleteMessages(AmazonSQS sqs, List<Message> messages) {
		System.out.println("Deleting received messages.\n");
		for (int i = 0; i < messages.size(); i++) {
			String messageReceiptHandle = messages.get(i).getReceiptHandle();
			sqs.deleteMessage(new DeleteMessageRequest(QUEUE_URL, messageReceiptHandle));
		}
	}

	/**
	 * Check a message for the mandatory fields
	 * @param message
	 * @return true if all fields are ok or false if there is at least one field missing
	 */
	public static boolean checkMessage(Message message) {
		Map<String, MessageAttributeValue> attrMap = message.getMessageAttributes();

		if (!attrMap.containsKey(QueueService.MARKET_ID_MESSAGE_ATTR)) return false;
		if (!attrMap.containsKey(QueueService.CITY_MESSAGE_ATTR)) return false;
		if (!attrMap.containsKey(QueueService.CITY_MESSAGE_ATTR)) return false;

		return true;
	}

}
