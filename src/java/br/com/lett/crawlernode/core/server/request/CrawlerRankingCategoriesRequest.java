package br.com.lett.crawlernode.core.server.request;

public class CrawlerRankingCategoriesRequest extends Request {
	
	private String location;
	private String categoryUrl;
	private Boolean takeAScreenshot;
	
	public CrawlerRankingCategoriesRequest() {
		super();
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getCategoryUrl() {
		return categoryUrl;
	}

	public void setCategoryUrl(String categoryUrl) {
		this.categoryUrl = categoryUrl;
	}
	
	public Boolean getTakeAScreenshot() {
		return takeAScreenshot;
	}

	public void setTakeAScreenshot(Boolean takeAScreenshot) {
		this.takeAScreenshot = takeAScreenshot;
	}

}
