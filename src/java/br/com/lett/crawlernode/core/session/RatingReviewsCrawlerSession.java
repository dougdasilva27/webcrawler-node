package br.com.lett.crawlernode.core.session;

import java.util.Map;

import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;

import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.queue.QueueService;

public class RatingReviewsCrawlerSession extends Session {

	/** Processed id associated with the sku being crawled */
	private Long processedId;

	/** Internal id associated with the sku being crawled */
	private String internalId;

	public RatingReviewsCrawlerSession(Message message, String queueName, Markets markets) {
		super(message, queueName, markets);
		
		Map<String, MessageAttributeValue> attrMap = message.getMessageAttributes();

		// setting processed id
		if (attrMap.containsKey(QueueService.PROCESSED_ID_MESSAGE_ATTR)) {
			this.processedId = Long.parseLong(attrMap.get(QueueService.PROCESSED_ID_MESSAGE_ATTR).getStringValue());
		}

		// setting internal id
		if (attrMap.containsKey(QueueService.INTERNAL_ID_MESSAGE_ATTR)) {
			this.internalId = attrMap.get(QueueService.INTERNAL_ID_MESSAGE_ATTR).getStringValue();
		}
	}
	
	@Override
	public Long getProcessedId() {
		return processedId;
	}

	public void setProcessedId(Long processedId) {
		this.processedId = processedId;
	}
	
	@Override
	public String getInternalId() {
		return internalId;
	}

	public void setInternalId(String internalId) {
		this.internalId = internalId;
	}
	
	@Override
	public String toString() {		
		StringBuilder stringBuilder = new StringBuilder();
		
		stringBuilder.append(super.toString());
		stringBuilder.append("internal id: " + internalId + "\n");
		stringBuilder.append("processed id: " + processedId + "\n");
		
		return stringBuilder.toString();
	}

}
