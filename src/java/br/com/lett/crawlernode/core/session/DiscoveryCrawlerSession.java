package br.com.lett.crawlernode.core.session;

import com.amazonaws.services.sqs.model.Message;

import br.com.lett.crawlernode.core.models.Markets;

public class DiscoveryCrawlerSession extends Session {
	
	public DiscoveryCrawlerSession(Message message, String queueName, Markets markets) {
		super(message, queueName, markets);
	}

}
