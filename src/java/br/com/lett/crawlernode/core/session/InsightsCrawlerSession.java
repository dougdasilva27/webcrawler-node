package br.com.lett.crawlernode.core.session;

import java.util.Map;

import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;

import br.com.lett.crawlernode.server.QueueService;

public class InsightsCrawlerSession extends CrawlerSession {

	/** Processed id associated with the sku being crawled */
	private Long processedId;

	/** Internal id associated with the sku being crawled */
	private String internalId;

	/** Number of truco checks */
	private int trucoAttemptsCounter;

	/** Number of readings to prevent a void status */
	private int voidAttemptsCounter;


	public InsightsCrawlerSession(Message message, String queueName) {
		super(message, queueName);

		Map<String, MessageAttributeValue> attrMap = message.getMessageAttributes();

		// initialize counters
		this.trucoAttemptsCounter = 0;
		this.voidAttemptsCounter = 0;

		// setting processed id
		if (attrMap.containsKey(QueueService.PROCESSED_ID_MESSAGE_ATTR)) {
			this.processedId = Long.parseLong(attrMap.get(QueueService.PROCESSED_ID_MESSAGE_ATTR).getStringValue());
		}

		// setting internal id
		if (attrMap.containsKey(QueueService.INTERNAL_ID_MESSAGE_ATTR)) {
			this.internalId = attrMap.get(QueueService.INTERNAL_ID_MESSAGE_ATTR).getStringValue();
		}

		// initialize counters
		this.trucoAttemptsCounter = 0;
		this.voidAttemptsCounter = 0;

	}
	
	@Override
	public Long getProcessedId() {
		return processedId;
	}

	public void setProcessedId(Long processedId) {
		this.processedId = processedId;
	}
	
	@Override
	public int getTrucoAttempts() {
		return trucoAttemptsCounter;
	}
	
	@Override
	public void incrementTrucoAttemptsCounter() {
		this.trucoAttemptsCounter++;
	}
	
	@Override
	public void incrementVoidAttemptsCounter() {
		this.voidAttemptsCounter++;
	}
	
	@Override
	public int getVoidAttempts() {
		return voidAttemptsCounter;
	}
	
	@Override
	public String getInternalId() {
		return internalId;
	}

	public void setInternalId(String internalId) {
		this.internalId = internalId;
	}
	
	public void setVoidAttempts(int voidAttempts) {
		this.voidAttemptsCounter = voidAttempts;
	}

}
