package br.com.lett.crawlernode.core.session.crawler;

import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.core.server.request.Request;
import br.com.lett.crawlernode.core.session.Session;

public class InsightsCrawlerSession extends Session {

	/** Processed id associated with the sku being crawled */
	private Long processedId;

	/** Internal id associated with the sku being crawled */
	private String internalId;

	/** Number of truco checks */
	private int trucoAttemptsCounter;

	/** Number of readings to prevent a void status */
	private int voidAttemptsCounter;


	public InsightsCrawlerSession(Request request, String queueName, Markets markets) {
		super(request, queueName, markets);
		
		processedId = request.getProcessedId();
		internalId = request.getInternalId();
		
		// initialize counters
		this.trucoAttemptsCounter = 0;
		this.voidAttemptsCounter = 0;

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
