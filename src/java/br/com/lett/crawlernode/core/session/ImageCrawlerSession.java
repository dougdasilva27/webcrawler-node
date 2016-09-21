package br.com.lett.crawlernode.core.session;

import com.amazonaws.services.sqs.model.Message;

public class ImageCrawlerSession extends CrawlerSession {
	
	public ImageCrawlerSession(Message message, String queueName) {
		super(message, queueName);
	}
	
}
