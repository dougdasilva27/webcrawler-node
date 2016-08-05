package br.com.lett.crawlernode.kernel;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.sqs.model.Message;

/**
 * This class is a work list that manages the messages retrieved
 * from the Amazon sqs.
 * @author Samir Leao
 *
 */
public class WorkList {
	protected static final Logger logger = LoggerFactory.getLogger(WorkList.class);
	
	public static final int DEFAULT_MAX_SIZE = 10;
	
	/**
	 * the internal list of messages
	 */
	private List<Message> messages;
	
	/**
	 * the maximum number of messages the work queue will handle
	 */
	private int maxSize;
	
	public WorkList(int maxSize) {
		messages = new ArrayList<Message>();
		this.maxSize = maxSize;
	}
	
	/**
	 * Add a list of messages in the work list. The messages
	 * are added until the work list reaches it`s maximum size
	 * @param messages the array of messages to be added on the work list
	 */
	public void addMessages(List<Message> messages) {
		for (Message message : messages) {
			if (this.messages.size() >= maxSize) return;
			this.messages.add(message);
		}
	}
	
	/**
	 * Add one message to the work list
	 * @param message the message to be added in the work list
	 */
	public void addMessage(Message message) {
		if (messages.size() < maxSize) {
			messages.add(message);
		}
	}
	
	/**
	 * Get a message from the work list to be processed
	 * @return
	 */
	public Message getMessage() {
		Message message = null;
		if (messages.size() > 0) {
			message = messages.get(0);
			messages.remove(0);
		}
		return message;
	}
	
	/**
	 * Compute the maximum number of messages to request from queue.
	 * This value will be passed to the QueueService, so it can fetch a number of
	 * messages that is between 0 and the maximum to fetch. This prevents that the QueueService
	 * doesn't fetch more messages that the work list can handle. If that occurs the extra messages
	 * will return to the sqs only when the visibility time expires, and other machine will get that
	 * task.
	 * @return the maximum number of messages to fetch from Amazon SQS
	 */
	public int maxMessagesToFetch() {
		int toFetch = maxSize - messages.size();
		if (toFetch < 0) return 0;
		return toFetch;
	}
	
	public boolean isEmpty() {
		return messages.isEmpty();
	}
	
	public int size() {
		return messages.size();
	}

	public int getMaxSize() {
		return maxSize;
	}

	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
	}

}
