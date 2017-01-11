package br.com.lett.crawlernode.queue;

import java.util.List;

import com.amazonaws.services.sqs.model.Message;

public class SQSRequestResult {
	
	private List<Message> messages;
	private String queueName;
	
	public SQSRequestResult() {
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
