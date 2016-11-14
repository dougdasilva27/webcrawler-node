package br.com.lett.crawlernode.core.session;

import com.amazonaws.services.sqs.model.Message;

import br.com.lett.crawlernode.core.models.Markets;

public class RatingReviewsCrawlerSession extends Session {
	
	public RatingReviewsCrawlerSession(Message message, String queueName, Markets markets) {
		super(message, queueName, markets);
	}

}
