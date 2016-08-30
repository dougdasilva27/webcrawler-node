package br.com.lett.crawlernode.server;

import java.util.List;

import com.amazonaws.services.sqs.model.Message;

public class RequestMessageResult {
	
	List<Message> messages;
	String queueName;
	
	public RequestMessageResult(List<Message> messages, String queueName) {
		this.messages = messages;
		this.queueName = queueName;
	}
	
	public List<Message> getMessages() {
		return this.messages;
	}
	
	public String getQueueName() {
		return this.queueName;
	}

}
