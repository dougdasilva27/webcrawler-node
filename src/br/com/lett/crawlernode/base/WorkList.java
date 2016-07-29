package br.com.lett.crawlernode.base;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.sqs.model.Message;

/**
 * This class is a work list that manages the messages retrieved
 * from the Amazon sqs.
 * @author Samir Leao
 *
 */
public class WorkList {
	
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
