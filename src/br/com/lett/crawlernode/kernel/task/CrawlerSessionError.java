package br.com.lett.crawlernode.kernel.task;

public class CrawlerSessionError {
	
	private String content;
	
	public CrawlerSessionError(String content) {
		this.content = content;
	}
	
	public String getErrorContent() {
		return content;
	}

}
