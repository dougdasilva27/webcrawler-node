package br.com.lett.crawlernode.kernel.task;

public class CrawlerSessionError {
	
	public static final String EXCEPTION = "exception";
	public static final String BUSINESS_LOGIC = "business_logic";
	
	/** The error content. Can be a String detailing the error, or a stack trace in case of an exception */
	private String content;
	
	/** The type of the error: exception | business logic */
	private String type;
	
	public CrawlerSessionError(String type, String content) {
		this.content = content;
		this.type = type;
	}
	
	public String getErrorContent() {
		return content;
	}

	public String getType() {
		return type;
	}

}
