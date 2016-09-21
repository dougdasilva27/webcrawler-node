package br.com.lett.crawlernode.core.session;

import java.util.ArrayList;
import java.util.Map;

import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;

import br.com.lett.crawlernode.server.QueueHandler;
import br.com.lett.crawlernode.server.QueueService;

public class InsightsCrawlerSession extends CrawlerSession {	

	public InsightsCrawlerSession(Message message, String queueName) {
		super(message, queueName);
		
//		Map<String, MessageAttributeValue> attrMap = message.getMessageAttributes();
//
//		// setting queue name
//		this.queueName = queueName;
//
//		// initialize counters
//		this.trucoAttemptsCounter = 0;
//		this.voidAttemptsCounter = 0;
//
//		// creating the errors list
//		this.crawlerSessionErrors = new ArrayList<CrawlerSessionError>();
//
//		// setting session id
//		this.sessionId = message.getMessageId();
//
//		// setting message receipt handle
//		this.setMessageReceiptHandle(message.getReceiptHandle());
//
//		// setting Market
//		this.market = new Market(message);
//
//		// setting URL and originalURL
//		this.url = message.getBody();
//		this.originalURL = message.getBody();
//
//		// setting processed id
//		if (attrMap.containsKey(QueueService.PROCESSED_ID_MESSAGE_ATTR)) {
//			this.processedId = Long.parseLong(attrMap.get(QueueService.PROCESSED_ID_MESSAGE_ATTR).getStringValue());
//		}
//
//		// setting internal id
//		if (attrMap.containsKey(QueueService.INTERNAL_ID_MESSAGE_ATTR)) {
//			this.internalId = attrMap.get(QueueService.INTERNAL_ID_MESSAGE_ATTR).getStringValue();
//		}
//
//		// type
//		if (queueName.equals(QueueHandler.INSIGHTS) || queueName.equals(QueueHandler.INSIGHTS_DEAD)) {
//			this.type = INSIGHTS_TYPE;
//		}
//		else if (queueName.equals(QueueHandler.SEED) || queueName.equals(QueueHandler.SEED_DEAD)) {
//			this.type = SEED_TYPE;
//		}
//		else if (queueName.equals(QueueHandler.DISCOVER) || queueName.equals(QueueHandler.DISCOVER_DEAD)) {
//			this.type = DISCOVERY_TYPE;
//		}
//		else if (queueName.equals(QueueHandler.DEVELOPMENT)) {
//			this.type = INSIGHTS_TYPE; // it's supposed to be the same as insights
//		}

	}


}
