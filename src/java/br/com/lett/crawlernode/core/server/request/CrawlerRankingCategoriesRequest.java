package br.com.lett.crawlernode.core.server.request;

public class CrawlerRankingCategoriesRequest extends Request {
	
	private String location;
	private String categoryUrl;
	
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

}
