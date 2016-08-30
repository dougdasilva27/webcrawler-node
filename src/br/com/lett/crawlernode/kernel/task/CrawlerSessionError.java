package br.com.lett.crawlernode.kernel.task;

public class CrawlerSessionError {
	
	public static final String EXCEPTION = "exception";
	
	private String content;
	private String type;
	
	public CrawlerSessionError(String type, String content) {
		this.content = content;
	}
	
	public String getErrorContent() {
		return content;
	}

	public String getType() {
		return type;
	}

}
