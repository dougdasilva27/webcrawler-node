package br.com.lett.crawlernode.core.server.request;

public class CrawlerRankingKeywordsRequest extends Request {
	
	private String location;
	
	public CrawlerRankingKeywordsRequest() {
		super();
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}
	

}
