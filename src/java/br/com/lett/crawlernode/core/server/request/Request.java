package br.com.lett.crawlernode.core.server.request;

public class Request {
	
	private String requestMethod;

	// common to all tasks
	private String messageId;
	private String messageBody;
	private String queueName;
	private String cityName;
	private String marketName;
	private int marketId;
	
	private String internalId;
	private Long processedId;	
	
	public Request() {
		super();
	}
	
	public String getMessageId() {
		return messageId;
	}
	
	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}
	
	public String getMessageBody() {
		return messageBody;
	}
	
	public void setMessageBody(String messageBody) {
		this.messageBody = messageBody.trim();
	}
	
	public String getQueueName() {
		return queueName;
	}
	
	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}
	
	public String getCityName() {
		return cityName;
	}

	public void setCityName(String cityName) {
		this.cityName = cityName;
	}

	public String getMarketName() {
		return marketName;
	}

	public void setMarketName(String marketName) {
		this.marketName = marketName;
	}
	
	public String getInternalId() {
		return internalId;
	}

	public void setInternalId(String internalId) {
		this.internalId = internalId;
	}

	public Long getProcessedId() {
		return processedId;
	}

	public void setProcessedId(Long processedId) {
		this.processedId = processedId;
	}
	
	public String getRequestMethod() {
		return requestMethod;
	}

	public void setRequestMethod(String requestMethod) {
		this.requestMethod = requestMethod;
	}
	
	public int getMarketId() {
		return marketId;
	}

	public void setMarketId(int marketId) {
		this.marketId = marketId;
	}
	
	@Override
	public String toString() {
		return "Request[messageId=" + messageId + 
				", messageBody=" + messageBody + 
				", queueName=" + queueName + 
				", cityName=" + cityName +
				", marketName=" + marketName + 
				", requestMethod=" + requestMethod +
				", internalId=" + internalId + 
				", processedId=" + processedId + 
				", marketId=" + marketId + "]";
	}
}
