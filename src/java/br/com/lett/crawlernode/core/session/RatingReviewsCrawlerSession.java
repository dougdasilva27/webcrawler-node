package br.com.lett.crawlernode.core.session;

import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.core.server.request.Request;

public class RatingReviewsCrawlerSession extends Session {

	/** Processed id associated with the sku being crawled */
	private Long processedId;

	/** Internal id associated with the sku being crawled */
	private String internalId;

	public RatingReviewsCrawlerSession(Request request, String queueName, Markets markets) {
		super(request, queueName, markets);
		
		processedId = request.getProcessedId();
		internalId = request.getInternalId();
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
		stringBuilder.append("internalId: " + internalId + "\n");
		stringBuilder.append("processedId: " + processedId + "\n");
		
		return stringBuilder.toString();
	}

}
