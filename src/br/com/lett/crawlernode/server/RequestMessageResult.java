package br.com.lett.crawlernode.server;

import java.util.List;

import com.amazonaws.services.sqs.model.Message;

public class RequestMessageResult {
	
	List<Message> messages;
	String queueName;
	
	public RequestMessageResult() {
		super();
	}
	
	public void setMessages(List<Message> messages) {
		this.messages = messages;
	}
	
	public List<Message> getMessages() {
		return this.messages;
	}
	
	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}
	
	public String getQueueName() {
		return this.queueName;
	}

}
