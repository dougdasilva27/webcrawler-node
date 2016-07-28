package br.com.lett.crawlernode.base;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.sqs.model.Message;

/**
 * 
 * @author Samir Leao
 *
 */
public class WorkList {

	private List<Message> messages;
	private int maxSize;
	
	public WorkList(int maxSize) {
		messages = new ArrayList<Message>();
		this.maxSize = maxSize;
	}
	
	public void addMessages(List<Message> messages) {
		// TODO
	}
	
	public void addMessage(Message message) {
		if (messages.size() < maxSize) {
			messages.add(message);
		}
	}
	
	public Message getMessage() {
		Message message = null;
		if (messages.size() > 0) {
			message = messages.get(0);
			messages.remove(0);
		}
		return message;
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
