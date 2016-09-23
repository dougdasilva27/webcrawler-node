package br.com.lett.crawlernode.core.session;

import com.amazonaws.services.sqs.model.Message;

public class DiscoveryCrawlerSession extends CrawlerSession {
	
	public DiscoveryCrawlerSession(Message message, String queueName) {
		super(message, queueName);
	}

}
