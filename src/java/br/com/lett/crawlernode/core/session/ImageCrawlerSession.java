package br.com.lett.crawlernode.core.session;

import com.amazonaws.services.sqs.model.Message;

import br.com.lett.crawlernode.core.models.Markets;

public class ImageCrawlerSession extends CrawlerSession {
	
	public ImageCrawlerSession(Message message, String queueName, Markets markets) {
		super(message, queueName, markets);
	}
	
}
