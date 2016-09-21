package br.com.lett.crawlernode.core.session;

import com.amazonaws.services.sqs.model.Message;

public class SeedCrawlerSession extends CrawlerSession {
	
	public SeedCrawlerSession(Message message, String queueName) {
		super(message, queueName);
	}

}
