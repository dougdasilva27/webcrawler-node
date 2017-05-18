package br.com.lett.crawlernode.core.server.request;

public class CrawlerRankingRequest extends Request {
	
	private String location;
	
	public CrawlerRankingRequest() {
		super();
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}
	

}
