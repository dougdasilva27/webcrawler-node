package br.com.lett.crawlernode.core.server.request;

public class CrawlerRankingKeywordsRequest extends Request {
	
	private String location;
	private Boolean takeAScreenshot;
	
	public CrawlerRankingKeywordsRequest() {
		super();
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public Boolean getTakeAScreenshot() {
		return takeAScreenshot;
	}

	public void setTakeAScreenshot(Boolean takeAScreenshot) {
		this.takeAScreenshot = takeAScreenshot;
	}
	

}
